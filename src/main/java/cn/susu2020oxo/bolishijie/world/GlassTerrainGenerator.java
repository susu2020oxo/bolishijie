package cn.susu2020oxo.bolishijie.world;

import cn.susu2020oxo.bolishijie.block.GlassTerrain;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.feature.EndSpikeFeature;

import java.util.ArrayList;
import java.util.List;

public final class GlassTerrainGenerator {
    private GlassTerrainGenerator() { }

    public static void convert(StructureWorldAccess world, Chunk chunk, StructureAccessor structures) {
        List<BlockBox> protectedAreas = new ArrayList<>();
        for (StructureStart start : structures.getStructureStarts(chunk.getPos(), structure -> true)) {
            for (StructurePiece piece : start.getChildren()) {
                protectedAreas.add(piece.getBoundingBox());
            }
        }
        // End spikes are features, not registered structures. Keep crystals, cages
        // and their supporting obsidian intact, including at chunk boundaries.
        if (world.toServerWorld().getRegistryKey() == World.END) {
            for (EndSpikeFeature.Spike spike : EndSpikeFeature.getSpikes(world)) {
                int r = spike.getRadius() + 1;
                protectedAreas.add(new BlockBox(spike.getCenterX() - r, 0, spike.getCenterZ() - r,
                        spike.getCenterX() + r, spike.getHeight() + 4, spike.getCenterZ() + r));
            }
        }

        int startX = chunk.getPos().getStartX();
        int startZ = chunk.getPos().getStartZ();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        ChunkSection[] sections = chunk.getSectionArray();
        // Desert wells and monster rooms are also features. Include neighboring
        // feature markers, so crossing a chunk boundary cannot cut a well in half.
        // The explicit FEATURES/1 dependency makes these reads legal and complete.
        for (int x = startX - 4; x < startX + 20; x++) {
            for (int z = startZ - 4; z < startZ + 20; z++) {
                for (int y = chunk.getBottomY(); y < chunk.getBottomY() + chunk.getHeight(); y++) {
                    pos.set(x, y, z);
                    Block marker = world.getBlockState(pos).getBlock();
                    if (marker == Blocks.SANDSTONE_SLAB || marker == Blocks.SPAWNER) {
                        int radius = marker == Blocks.SPAWNER ? 6 : 4;
                        protectedAreas.add(new BlockBox(x - radius, y - 4, z - radius,
                                x + radius, y + 4, z + radius));
                    }
                }
            }
        }

        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section.isEmpty()) {
                continue;
            }
            int bottomY = chunk.getBottomY() + i * 16;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 16; y++) {
                        BlockState before = section.getBlockState(x, y, z);
                        BlockState after = GlassTerrain.replacement(before);
                        if (before == after) {
                            continue;
                        }
                        pos.set(startX + x, bottomY + y, startZ + z);
                        if (!isProtected(pos, protectedAreas)) {
                            // Chunk setter updates heightmaps; no world neighbor callbacks
                            // and no fluid/entity mutations during terrain conversion.
                            chunk.setBlockState(pos, after);
                        }
                    }
                }
            }
        }
    }

    public static boolean isProtected(BlockPos pos, List<BlockBox> protectedAreas) {
        for (BlockBox box : protectedAreas) {
            if (box.contains(pos)) {
                return true;
            }
        }
        return false;
    }
}

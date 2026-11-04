package cn.susu2020oxo.bolishijie.block;

import cn.susu2020oxo.bolishijie.GlassWorld;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public final class GlassTerrain {
    private static final Map<Block, TerrainGlassBlock> VARIANTS = new IdentityHashMap<>();

    private GlassTerrain() { }

    public static void initialize() {
        // An explicit allowlist deliberately excludes ores, logs, leaves, fungi,
        // fluids, block entities, crops, portals and all decorative building blocks.
        for (String name : TerrainPalette.BLOCKS) {
            Identifier sourceId = Identifier.ofVanilla(name);
            if (!Registries.BLOCK.containsId(sourceId)) {
                throw new IllegalStateException("Unknown terrain block: " + sourceId);
            }
            Block source = Registries.BLOCK.get(sourceId);
            BlockState original = source.getDefaultState();
            Identifier id = GlassWorld.id("terrain/" + name);
            RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, id);
            // Do not copy requiresTool, random ticks, gravity or original loot tables.
            // Even bare hands drop a vanilla glass fragment. Bedrock stays unbreakable.
            AbstractBlock.Settings settings = AbstractBlock.Settings.create()
                    .registryKey(key).mapColor(MapColor.CLEAR).nonOpaque()
                    .strength(source == Blocks.BEDROCK ? -1.0F : 0.3F,
                            source == Blocks.BEDROCK ? 3600000.0F : 0.3F)
                    .sounds(BlockSoundGroup.GLASS)
                    .slipperiness(source.getSlipperiness())
                    .luminance(state -> original.getLuminance())
                    .allowsSpawning((state, world, pos, type) -> original.allowsSpawning(world, pos, type))
                    .solidBlock((state, world, pos) -> original.isSolidBlock(world, pos))
                    .suffocates((state, world, pos) -> original.shouldSuffocate(world, pos))
                    .blockVision((state, world, pos) -> original.shouldBlockVision(world, pos));
            TerrainGlassBlock glass = new TerrainGlassBlock(original, settings);
            Registry.register(Registries.BLOCK, key, glass);
            VARIANTS.put(source, glass);
        }
    }

    public static BlockState replacement(BlockState state) {
        TerrainGlassBlock glass = VARIANTS.get(state.getBlock());
        return glass == null || !state.getFluidState().isEmpty() ? state : glass.getDefaultState();
    }

    public static BlockState original(BlockState state) {
        return state.getBlock() instanceof TerrainGlassBlock glass ? glass.original() : state;
    }

    public static Collection<TerrainGlassBlock> blocks() {
        return Collections.unmodifiableCollection(VARIANTS.values());
    }
}

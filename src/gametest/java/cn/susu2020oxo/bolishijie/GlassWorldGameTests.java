package cn.susu2020oxo.bolishijie;

import cn.susu2020oxo.bolishijie.block.GlassTerrain;
import cn.susu2020oxo.bolishijie.block.TerrainGlassBlock;
import cn.susu2020oxo.bolishijie.item.GlassItems;
import cn.susu2020oxo.bolishijie.world.GlassTerrainGenerator;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.chunk.ChunkGenerationSteps;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.List;

/** These require the real Minecraft registries, Mixin and loaded data packs. */
public final class GlassWorldGameTests {
    @GameTest
    public void generationDeclaresStructureAndNeighborReads(TestContext context) {
        var spawn = ChunkGenerationSteps.GENERATION.get(ChunkStatus.SPAWN);
        check(context, spawn.getAdditionalLevel(ChunkStatus.STRUCTURE_STARTS) >= 8,
                "Structure protection must have a legal eight-chunk structure-start dependency");
        check(context, spawn.getAdditionalLevel(ChunkStatus.FEATURES) >= 1,
                "Neighbor feature markers must be complete and accessible before conversion");
        var loading = ChunkGenerationSteps.LOADING.get(ChunkStatus.SPAWN);
        check(context, loading.getAdditionalLevel(ChunkStatus.STRUCTURE_STARTS) < 8,
                "Loading an existing world must not add generation dependencies");
        context.complete();
    }

    @GameTest
    public void allThreeDimensionsHaveGlassTerrain(TestContext context) {
        for (Block source : List.of(Blocks.STONE, Blocks.GRASS_BLOCK, Blocks.NETHERRACK,
                Blocks.SOUL_SAND, Blocks.CRIMSON_NYLIUM, Blocks.END_STONE)) {
            check(context, GlassTerrain.replacement(source.getDefaultState()).getBlock()
                    instanceof TerrainGlassBlock, "Missing conversion: " + source);
        }
        context.complete();
    }

    @GameTest
    public void oresTreesFluidsAndBuildingsAreUnchanged(TestContext context) {
        for (Block block : List.of(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
                Blocks.ANCIENT_DEBRIS, Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE,
                Blocks.WATER, Blocks.LAVA, Blocks.OAK_LOG, Blocks.OAK_LEAVES,
                Blocks.WARPED_STEM, Blocks.WARPED_WART_BLOCK, Blocks.SHROOMLIGHT,
                Blocks.CHEST, Blocks.SPAWNER, Blocks.STONE_BRICKS, Blocks.END_PORTAL_FRAME)) {
            BlockState state = block.getDefaultState();
            check(context, GlassTerrain.replacement(state) == state, "Protected block changed: " + block);
        }
        context.complete();
    }

    @GameTest
    public void terrainKeepsLightBlockingAndCollision(TestContext context) {
        BlockPos pos = context.getAbsolutePos(new BlockPos(1, 1, 1));
        for (TerrainGlassBlock glass : GlassTerrain.blocks()) {
            BlockState state = glass.getDefaultState();
            check(context, state.getOpacity() == glass.original().getOpacity(), "Opacity changed: " + glass);
            check(context, state.getLuminance() == glass.original().getLuminance(), "Luminance changed: " + glass);
            check(context, state.getCollisionShape(context.getWorld(), pos).getBoundingBox().equals(
                    glass.original().getCollisionShape(context.getWorld(), pos).getBoundingBox()),
                    "Collision changed: " + glass);
        }
        context.complete();
    }

    @GameTest
    public void animalSpawnTagsAndGrassIdentitySurvive(TestContext context) {
        BlockState grass = GlassTerrain.replacement(Blocks.GRASS_BLOCK.getDefaultState());
        check(context, grass.isIn(BlockTags.ANIMALS_SPAWNABLE_ON), "Glass grass must support passive spawning");
        check(context, grass.isOf(Blocks.GRASS_BLOCK), "Grass-specific spawn/food checks must still work");
        BlockState stone = GlassTerrain.replacement(Blocks.STONE.getDefaultState());
        check(context, !stone.isIn(BlockTags.ANIMALS_SPAWNABLE_ON), "Do not allow animals on every glass surface");
        context.complete();
    }

    @GameTest
    public void surfaceSpawnPermissionsMatchOriginalTerrain(TestContext context) {
        BlockPos pos = context.getAbsolutePos(new BlockPos(1, 1, 1));
        for (TerrainGlassBlock glass : GlassTerrain.blocks()) {
            for (EntityType<?> type : List.of(EntityType.COW, EntityType.ZOMBIE, EntityType.ENDERMAN,
                    EntityType.PIGLIN, EntityType.GHAST, EntityType.SLIME)) {
                check(context, glass.getDefaultState().allowsSpawning(context.getWorld(), pos, type)
                        == glass.original().allowsSpawning(context.getWorld(), pos, type),
                        "Changed floor permission: " + glass + " / " + type);
            }
        }
        context.complete();
    }

    @GameTest
    public void realAnimalSpawnPredicateMatches(TestContext context) {
        BlockPos relativeFloor = new BlockPos(1, 1, 1);
        BlockPos above = context.getAbsolutePos(relativeFloor.up());
        context.setBlockState(relativeFloor, Blocks.GRASS_BLOCK.getDefaultState());
        boolean before = AnimalEntity.isValidNaturalSpawn(EntityType.COW, context.getWorld(),
                SpawnReason.NATURAL, above, Random.create(42));
        context.setBlockState(relativeFloor, GlassTerrain.replacement(Blocks.GRASS_BLOCK.getDefaultState()));
        boolean after = AnimalEntity.isValidNaturalSpawn(EntityType.COW, context.getWorld(),
                SpawnReason.NATURAL, above, Random.create(42));
        check(context, before == after, "Cow spawn predicate changed at the same position");
        context.complete();
    }

    @GameTest
    public void glassDropsVanillaGlassWithoutATool(TestContext context) {
        BlockPos pos = context.getAbsolutePos(new BlockPos(1, 1, 1));
        for (Block block : List.of(Blocks.STONE, Blocks.GRASS_BLOCK, Blocks.NETHERRACK, Blocks.END_STONE,
                Blocks.GLASS)) {
            BlockState state = GlassTerrain.replacement(block.getDefaultState());
            List<ItemStack> drops = Block.getDroppedStacks(state, context.getWorld(), pos, null,
                    null, ItemStack.EMPTY);
            check(context, drops.size() == 1 && drops.getFirst().isOf(Items.GLASS)
                    && drops.getFirst().getCount() == 1, "Expected one vanilla glass: " + block);
            check(context, !state.isToolRequired(), "Bare-hand glass mining must work");
        }
        BlockState bedrock = GlassTerrain.replacement(Blocks.BEDROCK.getDefaultState());
        check(context, bedrock.getHardness(context.getWorld(), pos) < 0, "Bedrock boundary became breakable");
        check(context, Block.getDroppedStacks(bedrock, context.getWorld(), pos, null).isEmpty(),
                "Bedrock must not drop shards");
        context.complete();
    }

    @GameTest
    public void toolsAndWearableArmorAreRegistered(TestContext context) {
        check(context, GlassItems.TOOLS.size() == 5 && GlassItems.EQUIPMENT.size() == 4,
                "Missing equipment registration");
        for (var item : GlassItems.TOOLS) {
            check(context, new ItemStack(item).getMaxDamage() == 96, "Glass tools need their own durability");
        }
        for (var item : GlassItems.EQUIPMENT) {
            ItemStack stack = new ItemStack(item);
            check(context, stack.contains(DataComponentTypes.EQUIPPABLE), "Armor must be wearable");
            check(context, stack.getMaxDamage() > 0, "Armor needs durability");
        }
        check(context, new ItemStack(Items.GLASS).isIn(GlassItems.REPAIR), "Vanilla glass must repair equipment");
        context.complete();
    }

    @GameTest
    public void structureProtectionCrossesChunkBoundaries(TestContext context) {
        List<BlockBox> boxes = List.of(new BlockBox(14, 60, 14, 20, 72, 20));
        check(context, GlassTerrainGenerator.isProtected(new BlockPos(16, 65, 16), boxes),
                "Structure piece across a chunk boundary must be protected");
        check(context, !GlassTerrainGenerator.isProtected(new BlockPos(21, 65, 16), boxes),
                "Unrelated terrain must remain convertible");
        context.complete();
    }

    private static void check(TestContext context, boolean condition, String message) {
        context.assertTrue(condition, Text.literal(message));
    }
}

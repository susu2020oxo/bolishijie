package cn.susu2020oxo.bolishijie.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.util.shape.VoxelShape;

/** Visually glass; its original terrain state supplies ecology and light blocking. */
public final class TerrainGlassBlock extends Block {
    private final BlockState original;

    public TerrainGlassBlock(BlockState original, Settings settings) {
        super(settings);
        this.original = original;
    }

    public BlockState original() {
        return original;
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return MapCodec.unit(this);
    }

    @Override
    protected int getOpacity(BlockState state) {
        return original.getOpacity();
    }

    @Override
    protected boolean isTransparent(BlockState state) {
        return original.isTransparent();
    }

    @Override
    protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return original.getCollisionShape(world, pos, context);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return original.getOutlineShape(world, pos, context);
    }

    @Override
    protected boolean isSideInvisible(BlockState state, BlockState neighbor, Direction direction) {
        // Also cull between different source materials: no seams between dirt/stone glass.
        return neighbor.getBlock() instanceof TerrainGlassBlock
                || super.isSideInvisible(state, neighbor, direction);
    }
}

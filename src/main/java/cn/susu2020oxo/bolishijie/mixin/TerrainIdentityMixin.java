package cn.susu2020oxo.bolishijie.mixin;

import cn.susu2020oxo.bolishijie.block.TerrainGlassBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve e.g. ANIMALS_SPAWNABLE_ON, fox/polar-bear terrain and grass identity.
 * No spawn predicate is forced true, so biome/light/height/space/mob caps still apply.
 */
@Mixin(AbstractBlock.AbstractBlockState.class)
abstract class TerrainIdentityMixin {
    @Inject(method = "isIn(Lnet/minecraft/registry/tag/TagKey;)Z", at = @At("HEAD"), cancellable = true)
    private void bolishijie$sourceTags(TagKey<Block> tag, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = (BlockState) (Object) this;
        if (state.getBlock() instanceof TerrainGlassBlock glass && glass.original().isIn(tag)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isOf(Lnet/minecraft/block/Block;)Z", at = @At("HEAD"), cancellable = true)
    private void bolishijie$sourceIdentity(Block block, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = (BlockState) (Object) this;
        if (state.getBlock() instanceof TerrainGlassBlock glass && glass.original().isOf(block)) {
            cir.setReturnValue(true);
        }
    }
}

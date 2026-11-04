package cn.susu2020oxo.bolishijie.mixin;

import cn.susu2020oxo.bolishijie.block.TerrainGlassBlock;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ShovelItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/** Vanilla tilling/path actions use a Block-keyed map instead of block tags. */
@Mixin({HoeItem.class, ShovelItem.class})
abstract class GroundToolMixin {
    @Redirect(method = "useOnBlock", at = @At(value = "INVOKE",
            target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object bolishijie$originalGroundAction(Map<?, ?> actions, Object block) {
        return actions.get(block instanceof TerrainGlassBlock glass ? glass.original().getBlock() : block);
    }
}

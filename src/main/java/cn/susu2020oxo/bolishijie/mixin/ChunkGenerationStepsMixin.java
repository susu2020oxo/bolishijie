package cn.susu2020oxo.bolishijie.mixin;

import net.minecraft.world.chunk.ChunkGenerationStep;
import net.minecraft.world.chunk.ChunkGenerationSteps;
import net.minecraft.world.chunk.ChunkStatus;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.function.UnaryOperator;

@Mixin(ChunkGenerationSteps.class)
abstract class ChunkGenerationStepsMixin {
    // Only change the generation pipeline. Existing FULL chunks still use the
    // unchanged loading pipeline, and are never converted or regenerated.
    @Redirect(method = "<clinit>",
            slice = @Slice(to = @At(value = "FIELD", opcode = Opcodes.PUTSTATIC,
                    target = "Lnet/minecraft/world/chunk/ChunkGenerationSteps;GENERATION:Lnet/minecraft/world/chunk/ChunkGenerationSteps;")),
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/ChunkGenerationSteps$Builder;then(Lnet/minecraft/world/chunk/ChunkStatus;Ljava/util/function/UnaryOperator;)Lnet/minecraft/world/chunk/ChunkGenerationSteps$Builder;"))
    private static ChunkGenerationSteps.Builder bolishijie$declareTerrainReads(
            ChunkGenerationSteps.Builder pipeline, ChunkStatus status,
            UnaryOperator<ChunkGenerationStep.Builder> factory) {
        if (status == ChunkStatus.SPAWN) {
            return pipeline.then(status, builder -> factory.apply(builder)
                    // Structure references can point to starts up to eight chunks away.
                    .dependsOn(ChunkStatus.STRUCTURE_STARTS, 8)
                    // Wait for neighbor feature writers and expose their markers.
                    .dependsOn(ChunkStatus.FEATURES, 1));
        }
        return pipeline.then(status, factory);
    }
}

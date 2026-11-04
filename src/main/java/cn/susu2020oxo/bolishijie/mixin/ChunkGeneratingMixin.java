package cn.susu2020oxo.bolishijie.mixin;

import cn.susu2020oxo.bolishijie.world.GlassTerrainGenerator;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkGenerating;
import net.minecraft.world.chunk.ChunkGenerationContext;
import net.minecraft.world.chunk.ChunkGenerationStep;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkGenerating.class)
abstract class ChunkGeneratingMixin {
    // The LIGHT dependency waits for neighboring feature writers to finish.
    // Convert just before initial entity population, retaining the already-computed
    // light by preserving source opacity/emission. Never scan old loaded chunks.
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "generateEntities", at = @At("HEAD"))
    private static void bolishijie$glassTerrain(ChunkGenerationContext context,
            ChunkGenerationStep step, BoundedRegionArray chunks, Chunk chunk,
            CallbackInfoReturnable<CompletableFuture<Chunk>> cir) {
        if (chunk.getStatus() != ChunkStatus.LIGHT) {
            return;
        }
        ChunkRegion region = new ChunkRegion(context.world(), chunks, step, chunk);
        GlassTerrainGenerator.convert(region, chunk,
                context.world().getStructureAccessor().forRegion(region));
    }
}

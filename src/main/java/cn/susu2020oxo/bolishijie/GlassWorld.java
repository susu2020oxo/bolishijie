package cn.susu2020oxo.bolishijie;

import cn.susu2020oxo.bolishijie.block.GlassTerrain;
import cn.susu2020oxo.bolishijie.item.GlassItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public final class GlassWorld implements ModInitializer {
    public static final String ID = "bolishijie";

    public static Identifier id(String path) {
        return Identifier.of(ID, path);
    }

    @Override
    public void onInitialize() {
        GlassTerrain.initialize();
        GlassItems.initialize();
    }
}

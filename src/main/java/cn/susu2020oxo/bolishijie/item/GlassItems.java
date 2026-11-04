package cn.susu2020oxo.bolishijie.item;

import cn.susu2020oxo.bolishijie.GlassWorld;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class GlassItems {
    public static final TagKey<Item> REPAIR = TagKey.of(RegistryKeys.ITEM, GlassWorld.id("glass_fragments"));
    public static final ToolMaterial MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL, 96, 6.0F, 2.0F, 18, REPAIR);
    public static final ArmorMaterial ARMOR = new ArmorMaterial(8,
            Map.of(EquipmentType.HELMET, 2, EquipmentType.CHESTPLATE, 6,
                    EquipmentType.LEGGINGS, 5, EquipmentType.BOOTS, 2, EquipmentType.BODY, 6),
            18, SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, 0.0F, 0.0F, REPAIR,
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, GlassWorld.id("glass")));
    public static final List<Item> TOOLS = new ArrayList<>();
    public static final List<Item> EQUIPMENT = new ArrayList<>();

    private GlassItems() { }

    public static void initialize() {
        TOOLS.add(register("glass_pickaxe", settings -> new Item(settings.pickaxe(MATERIAL, 1.0F, -2.8F))));
        TOOLS.add(register("glass_sword", settings -> new Item(settings.sword(MATERIAL, 3.0F, -2.4F))));
        TOOLS.add(register("glass_axe", settings -> new AxeItem(MATERIAL, 6.0F, -3.1F, settings)));
        TOOLS.add(register("glass_shovel", settings -> new ShovelItem(MATERIAL, 1.5F, -3.0F, settings)));
        TOOLS.add(register("glass_hoe", settings -> new HoeItem(MATERIAL, -2.0F, -1.0F, settings)));
        EQUIPMENT.add(register("glass_helmet", settings -> new Item(settings.armor(ARMOR, EquipmentType.HELMET))));
        EQUIPMENT.add(register("glass_chestplate", settings -> new Item(settings.armor(ARMOR, EquipmentType.CHESTPLATE))));
        EQUIPMENT.add(register("glass_leggings", settings -> new Item(settings.armor(ARMOR, EquipmentType.LEGGINGS))));
        EQUIPMENT.add(register("glass_boots", settings -> new Item(settings.armor(ARMOR, EquipmentType.BOOTS))));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> TOOLS.forEach(entries::add));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(TOOLS.get(1));
            EQUIPMENT.forEach(entries::add);
        });
    }

    private static Item register(String name, Function<Item.Settings, Item> factory) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, GlassWorld.id(name));
        return Registry.register(Registries.ITEM, key, factory.apply(new Item.Settings().registryKey(key)));
    }
}

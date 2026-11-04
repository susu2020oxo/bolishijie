#!/usr/bin/env python3
"""Offline asset/recipe contract checks. These do NOT replace Minecraft GameTests."""
import json
from pathlib import Path
import re
import struct
import unittest
import zlib

from generate_resources import PATTERNS, PALETTE, RES, ASSETS, DATA


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


class ResourceTests(unittest.TestCase):
    def test_every_json_parses(self):
        files = list(RES.rglob("*.json"))
        self.assertGreater(len(files), 100)
        for path in files:
            with self.subTest(path=path):
                self.assertIsInstance(load(path), dict)

    def test_exact_minecraft_version(self):
        mod = load(RES / "fabric.mod.json")
        self.assertEqual(mod["depends"]["minecraft"], "1.21.10")
        self.assertEqual(mod["environment"], "*")

    def test_terrain_palette_excludes_ores_trees_fluids(self):
        names = re.findall(r'"([a-z_]+)"', PALETTE.read_text())
        self.assertEqual(len(names), len(set(names)))
        self.assertTrue({"stone", "netherrack", "end_stone"} <= set(names))
        for name in names:
            self.assertFalse(any(suffix in name for suffix in ("_ore", "_log", "_leaves", "_stem")))
            self.assertNotIn(name, {"water", "lava", "ancient_debris", "warped_wart_block",
                                    "nether_wart_block", "shroomlight", "chest", "spawner"})

    def test_every_terrain_variant_has_model_and_loot(self):
        for name in re.findall(r'"([a-z_]+)"', PALETTE.read_text()):
            state = load(ASSETS / f"blockstates/terrain/{name}.json")
            self.assertEqual(state["variants"][""]["model"], "bolishijie:block/terrain_glass")
            loot = load(DATA / f"loot_table/blocks/terrain/{name}.json")
            if name == "bedrock":
                self.assertEqual(loot["pools"], [])
            else:
                self.assertEqual(loot["pools"][0]["entries"][0]["name"], "minecraft:glass")
                self.assertEqual(loot["pools"][0]["rolls"], 1)
                self.assertEqual(loot["pools"][0]["conditions"],
                                 [{"condition": "minecraft:survives_explosion"}])

    def test_normal_glass_can_be_mined_again(self):
        loot = load(RES / "data/minecraft/loot_table/blocks/glass.json")
        self.assertEqual(loot["pools"][0]["entries"][0]["name"], "minecraft:glass")

    def test_nine_vanilla_shaped_recipes(self):
        files = list((DATA / "recipe").glob("*.json"))
        self.assertEqual(len(files), 9)
        for item, pattern in PATTERNS.items():
            recipe = load(DATA / f"recipe/{item}.json")
            self.assertEqual(recipe["pattern"], pattern)
            self.assertEqual(recipe["key"]["G"], "minecraft:glass")
            self.assertEqual(recipe["result"], {"id": f"bolishijie:{item}", "count": 1})
            for row in pattern:
                self.assertEqual(len(row), len(pattern[0]))
                for symbol in row.strip():
                    if symbol != " ":
                        self.assertIn(symbol, recipe["key"])

    def test_modern_item_models_exist(self):
        for item in PATTERNS:
            definition = load(ASSETS / f"items/{item}.json")
            self.assertEqual(definition["model"]["model"], f"bolishijie:item/{item}")
            model = load(ASSETS / f"models/item/{item}.json")
            self.assertEqual(model["textures"]["layer0"], f"bolishijie:item/{item}")
            self.assertTrue((ASSETS / f"textures/item/{item}.png").is_file())

    def test_recipe_unlocks_match_recipes(self):
        for item in PATTERNS:
            advancement = load(DATA / f"advancement/recipes/{item}.json")
            self.assertEqual(advancement["rewards"]["recipes"], [f"bolishijie:{item}"])
            self.assertEqual(advancement["requirements"], [["has_glass"]])

    def test_png_dimensions_checksums_and_transparency(self):
        files = list((ASSETS / "textures").rglob("*.png"))
        self.assertEqual(len(files), 11)
        for path in files:
            with self.subTest(path=path):
                data = path.read_bytes()
                self.assertEqual(data[:8], b"\x89PNG\r\n\x1a\n")
                pos, compressed = 8, b""
                while pos < len(data):
                    size = struct.unpack(">I", data[pos:pos + 4])[0]
                    kind = data[pos + 4:pos + 8]
                    body = data[pos + 8:pos + 8 + size]
                    crc = struct.unpack(">I", data[pos + 8 + size:pos + 12 + size])[0]
                    self.assertEqual(zlib.crc32(kind + body), crc)
                    if kind == b"IHDR":
                        width, height, depth, color, *_ = struct.unpack(">IIBBBBB", body)
                        self.assertEqual((width, height), (16, 16) if "/item/" in str(path) else (64, 32))
                        self.assertEqual((depth, color), (8, 6))
                    elif kind == b"IDAT":
                        compressed += body
                    pos += 12 + size
                raw = zlib.decompress(compressed)
                self.assertEqual(len(raw), height * (1 + 4 * width))
                alpha = [raw[y * (1 + 4 * width) + 1 + x * 4 + 3]
                         for y in range(height) for x in range(width)]
                self.assertIn(0, alpha)
                self.assertTrue(any(a > 0 for a in alpha))

    def test_armor_render_layers_exist(self):
        equipment = load(ASSETS / "equipment/glass.json")
        for layer in ("humanoid", "humanoid_leggings"):
            self.assertEqual(equipment["layers"][layer], [{"texture": "bolishijie:glass"}])
            self.assertTrue((ASSETS / f"textures/entity/equipment/{layer}/glass.png").is_file())

    def test_names_in_both_languages(self):
        for language in ("zh_cn", "en_us"):
            entries = load(ASSETS / f"lang/{language}.json")
            for item in PATTERNS:
                self.assertTrue(entries[f"item.bolishijie.{item}"])

    def test_vanilla_glass_repairs_equipment(self):
        tag = load(DATA / "tags/item/glass_fragments.json")
        self.assertEqual(tag["values"], ["minecraft:glass"])

    def test_all_mixins_have_source_files(self):
        settings = load(RES / "bolishijie.mixins.json")
        root = RES.parent / "java/cn/susu2020oxo/bolishijie/mixin"
        for name in settings["mixins"]:
            self.assertTrue((root / f"{name}.java").is_file())


if __name__ == "__main__":
    unittest.main(verbosity=2)

#!/usr/bin/env python3
"""Generate reproducible 1.21.10 resources using only the Python standard library."""
import json
from pathlib import Path
import re
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/bolishijie"
DATA = RES / "data/bolishijie"
PALETTE = ROOT / "src/main/java/cn/susu2020oxo/bolishijie/block/TerrainPalette.java"

PATTERNS = {
    "glass_pickaxe": ["GGG", " S ", " S "],
    "glass_sword": ["G", "G", "S"],
    "glass_axe": ["GG", "GS", " S"],
    "glass_shovel": ["G", "S", "S"],
    "glass_hoe": ["GG", " S", " S"],
    "glass_helmet": ["GGG", "G G"],
    "glass_chestplate": ["G G", "GGG", "GGG"],
    "glass_leggings": ["GGG", "G G", "G G"],
    "glass_boots": ["G G", "G G"],
}
NAMES = ["玻璃镐", "玻璃剑", "玻璃斧", "玻璃锹", "玻璃锄",
         "玻璃头盔", "玻璃胸甲", "玻璃护腿", "玻璃靴子"]

# Original pixel art. '.' transparent, G blue edge, g glass, W highlight, S wood.
ICONS = {
    "glass_pickaxe": ["..GGGGGGGGG.....", ".GWWgggggggG....", "..GGGGgGgggG....",
                      "......SG..gG....", ".....SS...GG....", "....SS..........",
                      "...SS...........", "..SS............", ".SS............."],
    "glass_sword": ["...........G....", "..........GWG...", ".........GWgG...",
                    "........GWgG....", ".......GWgG.....", "......GWgG......",
                    "...G.GWgG.......", "...GGggG........", "....GGG.........",
                    "...SS.GG........", "..SS...G........", ".SS............."],
    "glass_axe": [".......GGGG.....", "......GWgggG....", ".....GWggggG....",
                  ".....GgggGG.....", "......GGSS......", ".......SS.......",
                  "......SS........", ".....SS.........", "....SS..........",
                  "...SS...........", "..SS............"],
    "glass_shovel": [".........GGG....", "........GWggG...", ".......GWgggG...",
                     ".......GgggG....", "........GGS.....", "........SS......",
                     ".......SS.......", "......SS........", ".....SS.........",
                     "....SS..........", "...SS..........."],
    "glass_hoe": ["....GGGGGGGG....", "...GWgggggggG...", "....GGGGSGGG....",
                  ".......SS..G....", "......SS........", ".....SS.........",
                  "....SS..........", "...SS...........", "..SS............"],
    "glass_helmet": ["....GGGGGGGG....", "...GWgggggggG...", "..GWgggggggggG..",
                     "..GggGGGGGGggG..", "..GgG......GgG..", "..GgG......GgG..",
                     "..GG........GG.."],
    "glass_chestplate": ["..GGG......GGG..", ".GWgG......GggG.", ".GgggGGGGGGgggG.",
                         ".GGGggggggggGGG.", "...GWgggggggG...", "...GgggWggggG...",
                         "...GggggggggG...", "...GggggggggG...", "...GGGGGGGGGG..."],
    "glass_leggings": ["...GGGGGGGGGG...", "...GWgggggggG...", "...GggggggggG...",
                       "...GggGGGGggG...", "...GggG..GggG...", "...GggG..GggG...",
                       "...GggG..GggG...", "...GggG..GggG...", "...GGGG..GGGG..."],
    "glass_boots": ["...GGGG..GGGG...", "...GWgG..GWgG...", "...GggG..GggG...",
                    "...GggG..GggG...", "..GgggG..GgggG..", "..GgggG..GgggG..",
                    "..GGGGG..GGGGG.."],
}


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def png(path, width, height, pixels):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))
    raw = b"".join(b"\0" + bytes(sum(row, ())) for row in pixels)
    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def generate():
    names = re.findall(r'"([a-z_]+)"', PALETTE.read_text(encoding="utf-8"))
    for name in names:
        write_json(ASSETS / f"blockstates/terrain/{name}.json", {
            "variants": {"": {"model": "bolishijie:block/terrain_glass"}}})
        write_json(DATA / f"loot_table/blocks/terrain/{name}.json", glass_loot(empty=name == "bedrock"))
    write_json(ASSETS / "models/block/terrain_glass.json", {
        "parent": "minecraft:block/cube_all", "ambientocclusion": False,
        "textures": {"all": "minecraft:block/glass"}})
    # Shards are literally the vanilla glass item, including when it is placed again.
    write_json(RES / "data/minecraft/loot_table/blocks/glass.json", glass_loot())
    write_json(DATA / "tags/item/glass_fragments.json", {"replace": False, "values": ["minecraft:glass"]})
    write_json(ASSETS / "equipment/glass.json", {"layers": {
        "humanoid": [{"texture": "bolishijie:glass"}],
        "humanoid_leggings": [{"texture": "bolishijie:glass"}]}})

    zh, en = {}, {}
    colors = {".": (0, 0, 0, 0), "G": (90, 169, 187, 255), "g": (183, 227, 237, 190),
              "W": (240, 255, 255, 255), "S": (129, 94, 54, 255)}
    for (item, pattern), label in zip(PATTERNS.items(), NAMES):
        zh[f"item.bolishijie.{item}"] = label
        en[f"item.bolishijie.{item}"] = item.replace("_", " ").title()
        keys = {"G": "minecraft:glass"}
        if any("S" in row for row in pattern):
            keys["S"] = "minecraft:stick"
        write_json(DATA / f"recipe/{item}.json", {
            "type": "minecraft:crafting_shaped", "category": "equipment", "key": keys,
            "pattern": pattern, "result": {"id": f"bolishijie:{item}", "count": 1}})
        write_json(DATA / f"advancement/recipes/{item}.json", {
            "parent": "minecraft:recipes/root",
            "criteria": {"has_glass": {"trigger": "minecraft:inventory_changed",
                                       "conditions": {"items": [{"items": "minecraft:glass"}]}}},
            "requirements": [["has_glass"]], "rewards": {"recipes": [f"bolishijie:{item}"]}})
        write_json(ASSETS / f"items/{item}.json", {
            "model": {"type": "minecraft:model", "model": f"bolishijie:item/{item}"}})
        write_json(ASSETS / f"models/item/{item}.json", {
            "parent": "minecraft:item/handheld" if "S" in keys else "minecraft:item/generated",
            "textures": {"layer0": f"bolishijie:item/{item}"}})
        rows = ICONS[item]
        rows = ["." * 16] * 2 + rows
        rows += ["." * 16] * (16 - len(rows))
        png(ASSETS / f"textures/item/{item}.png", 16, 16,
            [[colors[c] for c in row.ljust(16, ".")] for row in rows])

    for name in names:
        zh[f"block.bolishijie.terrain.{name}"] = "地形玻璃"
        en[f"block.bolishijie.terrain.{name}"] = "Terrain Glass"
    write_json(ASSETS / "lang/zh_cn.json", zh)
    write_json(ASSETS / "lang/en_us.json", en)

    # A transparent armor shell with icy edges and small glints. The gaps keep the
    # player skin visible even on equipment render layers using alpha testing.
    for layer in ("humanoid", "humanoid_leggings"):
        pixels = [[(0, 0, 0, 0) for x in range(64)] for y in range(32)]
        for y in range(32):
            for x in range(64):
                if x % 8 in (0, 7) or y % 8 in (0, 7):
                    pixels[y][x] = (158, 219, 235, 220)
                elif (x + y) % 13 == 0:
                    pixels[y][x] = (239, 255, 255, 255)
        png(ASSETS / f"textures/entity/equipment/{layer}/glass.png", 64, 32, pixels)


def glass_loot(empty=False):
    return {"type": "minecraft:block", "pools": [] if empty else [{
        "rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:glass"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]}


if __name__ == "__main__":
    generate()

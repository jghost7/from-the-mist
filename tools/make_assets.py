"""Writes the mod's JSON: blockstates, models, item definitions, language, recipe, loot tables.

Run from the project root:  python tools/make_assets.py
"""
import json
import os

RES = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
NS = "fromthemist"


def write(rel, data):
    path = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


# ---------------------------------------------------------------- talisman block
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
variants = {}
for facing, y in ROT.items():
    for charge in range(8):
        model = {"model": f"{NS}:block/talisman_cracked" if charge <= 1 else f"{NS}:block/talisman"}
        if y:
            model["y"] = y
        variants[f"charge={charge},facing={facing}"] = model
write(f"assets/{NS}/blockstates/talisman.json", {"variants": variants})


def talisman_model(texture):
    side = {"uv": [2, 1, 3, 15], "texture": "#face"}
    return {
        "textures": {"face": texture, "particle": texture},
        "elements": [{
            "from": [4, 3, 15], "to": [12, 13, 16],
            "faces": {
                "north": {"uv": [2, 1, 14, 15], "texture": "#face"},
                "south": {"uv": [2, 1, 14, 15], "texture": "#face"},
                "east": side, "west": side,
                "up": {"uv": [2, 1, 14, 2], "texture": "#face"},
                "down": {"uv": [2, 14, 14, 15], "texture": "#face"},
            },
        }],
    }


write(f"assets/{NS}/models/block/talisman.json", talisman_model(f"{NS}:block/talisman"))
write(f"assets/{NS}/models/block/talisman_cracked.json", talisman_model(f"{NS}:block/talisman_cracked"))

# ---------------------------------------------------------------- items
for item in ("talisman", "mist_essence"):
    write(f"assets/{NS}/items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{item}"}})
    write(f"assets/{NS}/models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{item}"}})

# ---------------------------------------------------------------- language
write(f"assets/{NS}/lang/en_us.json", {
    "block.fromthemist.talisman": "Talisman",
    "item.fromthemist.mist_essence": "Mist Essence",
    "entity.fromthemist.hollow": "Hollow",
    "entity.fromthemist.skitterer": "Skitterer",
    "entity.fromthemist.stinger": "Stinger",
    "entity.fromthemist.leviathan": "Strider",
    "entity.fromthemist.colossus": "Colossus",
    "screen.fromthemist.character": "Survivor",
    "screen.fromthemist.level": "Level %s",
    "screen.fromthemist.points": "Talent points: %s",
    "screen.fromthemist.max": "Nothing more to learn",
    "screen.fromthemist.skills": "Skills (they rise as you use them)",
    "screen.fromthemist.cooldown": "ready in %ss",
    "screen.fromthemist.ability_needs": "Ability. Needs %s points in this tree. Key: %s",
    "key.category.fromthemist.main": "From the Mist",
    "key.fromthemist.character": "Character screen",
    "key.fromthemist.ability_warden": "Ward Pulse",
    "key.fromthemist.ability_hunter": "Clear Air",
    "key.fromthemist.ability_survivor": "Adrenaline",
    "tree.fromthemist.warden": "Warden",
    "tree.fromthemist.hunter": "Hunter",
    "tree.fromthemist.survivor": "Survivor",
    "talent.fromthemist.steady_hands": "Steady Hands",
    "talent.fromthemist.steady_hands.desc": "Each mist essence feeds a talisman one more night per rank.",
    "talent.fromthemist.quiet_step": "Quiet Step",
    "talent.fromthemist.quiet_step.desc": "The Hollow sense you from 15% less far per rank.",
    "talent.fromthemist.thick_skin": "Thick Skin",
    "talent.fromthemist.thick_skin.desc": "Take 8% less damage from the Hollow per rank.",
    "talent.fromthemist.keeper": "Keeper",
    "talent.fromthemist.keeper.desc": "15% chance per rank that a talisman near you loses nothing at dawn.",
    "talent.fromthemist.ward_pulse": "Ward Pulse",
    "talent.fromthemist.ward_pulse.desc": "Throws back every Hollow within 12 blocks and slows them to a crawl for 8 seconds. 3 minute cooldown.",
    "talent.fromthemist.mist_eyes": "Mist Eyes",
    "talent.fromthemist.mist_eyes.desc": "See 25% further in the mist per rank.",
    "talent.fromthemist.cull": "Cull",
    "talent.fromthemist.cull.desc": "Deal 10% more damage to mist creatures per rank.",
    "talent.fromthemist.harvester": "Harvester",
    "talent.fromthemist.harvester.desc": "15% chance per rank of an extra mist essence from a mist kill.",
    "talent.fromthemist.antivenom": "Antivenom",
    "talent.fromthemist.antivenom.desc": "Stinger poison lasts half as long. At rank 2 you are immune.",
    "talent.fromthemist.clear_air": "Clear Air",
    "talent.fromthemist.clear_air.desc": "For 20 seconds the mist thins around you and its creatures within 32 blocks show through it. 4 minute cooldown.",
    "talent.fromthemist.vigor": "Vigor",
    "talent.fromthemist.vigor.desc": "One more heart per rank.",
    "talent.fromthemist.fleet": "Fleet",
    "talent.fromthemist.fleet.desc": "Move 4% faster on foot per rank.",
    "talent.fromthemist.quick_study": "Quick Study",
    "talent.fromthemist.quick_study.desc": "Gain 10% more experience of every kind per rank.",
    "talent.fromthemist.tough": "Tough",
    "talent.fromthemist.tough.desc": "2 armour per rank.",
    "talent.fromthemist.adrenaline": "Adrenaline",
    "talent.fromthemist.adrenaline.desc": "8 seconds of Speed II and Resistance. 3 minute cooldown.",
    "skill.fromthemist.combat": "Combat",
    "skill.fromthemist.combat.bonus": "+%s%% melee damage",
    "skill.fromthemist.archery": "Archery",
    "skill.fromthemist.archery.bonus": "+%s%% projectile damage",
    "skill.fromthemist.mining": "Mining",
    "skill.fromthemist.mining.bonus": "+%s%% digging speed",
    "skill.fromthemist.athletics": "Athletics",
    "skill.fromthemist.athletics.bonus": "+%s%% movement speed",
    "skill.fromthemist.endurance": "Endurance",
    "skill.fromthemist.endurance.bonus": "-%s%% damage taken",
    "message.fromthemist.level_up": "You are level %s. A talent point is waiting.",
    "message.fromthemist.skill_up": "%s is now %s.",
    "message.fromthemist.ability_cooldown": "Not ready for another %s seconds.",
    "message.fromthemist.welcome": "You find a carved stone in your pack. Hang it inside by the door, and shut the door before dark.",
    "message.fromthemist.dusk": "The light is going. Get inside.",
    "message.fromthemist.mist_coming": "A bank of mist is moving in from the %s.",
    "message.fromthemist.mist_here": "The mist is on you.",
    "message.fromthemist.mist_near": "Mist about %s blocks to the %s",
    "message.fromthemist.mist_leaving": "The mist is thinning.",
    "message.fromthemist.dawn": "Dawn. Night %s is behind you.",
    "message.fromthemist.escalate_1": "They were quicker tonight.",
    "message.fromthemist.escalate_2": "They are done being patient. Stone will not hold them long.",
    "message.fromthemist.escalate_3": "There are more of them every night, and they do not stroll any more.",
    "message.fromthemist.escalate_4": "Nothing you can build will slow them now. Only the ward.",
    "direction.fromthemist.north": "north",
    "direction.fromthemist.north_east": "north-east",
    "direction.fromthemist.east": "east",
    "direction.fromthemist.south_east": "south-east",
    "direction.fromthemist.south": "south",
    "direction.fromthemist.south_west": "south-west",
    "direction.fromthemist.west": "west",
    "direction.fromthemist.north_west": "north-west",
    "message.fromthemist.sleep_mist": "You can't sleep with the mist against the windows.",
    "message.fromthemist.sleep_unwarded": "You can't sleep here. Hang a talisman and seal the room.",
    "message.fromthemist.talisman_sealed": "The room is sealed. The talisman will hold for %s more night(s).",
    "message.fromthemist.talisman_open": "The room is not sealed. Close every door and gap. (%s night(s) left in the stone)",
    "message.fromthemist.talisman_dead": "The talisman is cold. Feed it mist essence.",
    "message.fromthemist.talisman_full": "The talisman will take no more.",
    "message.fromthemist.talisman_fed": "The carving warms. %s night(s) in the stone.",
    "message.fromthemist.talisman_last_night": "A talisman has cracked. It has one night left in it.",
    "message.fromthemist.talisman_spent": "A talisman has gone cold.",
    "message.fromthemist.talisman_wrong_place": "The talisman does nothing here.",
})

# ---------------------------------------------------------------- recipe
write(f"data/{NS}/recipe/talisman.json", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "key": {"S": "minecraft:string", "#": "minecraft:stone", "E": f"{NS}:mist_essence"},
    "pattern": [" S ", "#E#", " # "],
    "result": {"id": f"{NS}:talisman"},
})

# ---------------------------------------------------------------- loot
write(f"data/{NS}/loot_table/blocks/talisman.json", {
    "type": "minecraft:block",
    "pools": [{
        "rolls": 1,
        "entries": [{
            "type": "minecraft:item",
            "name": f"{NS}:talisman",
            "modifier": [{"type": "minecraft:copy_state", "block": f"{NS}:talisman", "properties": ["charge"]}],
        }],
    }],
})


def stack(item, low, high, weight):
    return {"type": "minecraft:item", "name": item, "weight": weight,
            "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}]}


def mist_loot(essence_low, essence_high):
    """Essence every time, and when a player made the kill, a good chance of something worth the trip."""
    return {
        "type": "minecraft:entity",
        "pools": [
            {
                "rolls": 1,
                "entries": [{
                    "type": "minecraft:item",
                    "name": f"{NS}:mist_essence",
                    "modifier": [
                        {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": essence_low, "max": essence_high}},
                        {"type": "minecraft:enchanted_count_increase", "count": {"type": "minecraft:uniform", "min": 0.0, "max": 1.0},
                         "enchantment": "minecraft:looting"},
                    ],
                }],
            },
            {
                "rolls": 1,
                "condition": {"type": "minecraft:killed_by_player"},
                "entries": [
                    {"type": "minecraft:empty", "weight": 30},
                    stack("minecraft:iron_ingot", 3, 6, 26),
                    stack("minecraft:gold_ingot", 2, 5, 16),
                    stack("minecraft:emerald", 2, 4, 12),
                    stack("minecraft:diamond", 1, 2, 10),
                    stack("minecraft:experience_bottle", 2, 4, 4),
                    stack("minecraft:netherite_scrap", 1, 1, 2),
                ],
            },
        ],
    }


write(f"data/{NS}/loot_table/entities/skitterer.json", mist_loot(1, 2))
write(f"data/{NS}/loot_table/entities/stinger.json", mist_loot(1, 3))

# ---------------------------------------------------------------- what essence buys
write(f"data/{NS}/recipe/enchanted_golden_apple.json", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "key": {"E": f"{NS}:mist_essence", "A": "minecraft:golden_apple"},
    "pattern": ["EEE", "EAE", "EEE"],
    "result": {"id": "minecraft:enchanted_golden_apple"},
})
write(f"data/{NS}/recipe/mending_book.json", {
    "type": "minecraft:crafting_shapeless",
    "category": "misc",
    "ingredients": [f"{NS}:mist_essence"] * 4 + ["minecraft:book"],
    "result": {"id": "minecraft:enchanted_book", "components": {"minecraft:stored_enchantments": {"minecraft:mending": 1}}},
})
write(f"data/{NS}/recipe/totem_of_undying.json", {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "key": {"E": f"{NS}:mist_essence", "G": "minecraft:gold_block", "M": "minecraft:emerald"},
    "pattern": ["EME", "EGE", " E "],
    "result": {"id": "minecraft:totem_of_undying"},
})

print("assets written")

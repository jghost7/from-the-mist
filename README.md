# From the Mist

A horror survival mod for Minecraft Java 26.3 (Fabric). The goal is to last as many nights as you can.

## The loop

1. **Clear days are for work.** Gather, build, and get a room ready: walls, a roof, a door that shuts.
2. **Every night the Hollow walk.** They look like people and nothing kills them. They know where you are through
   any wall and will pull a house apart to reach you. A bell and a message warn you at dusk.
3. **A talisman keeps them out.** Hang it on a wall inside. It wards the room only while the room is *sealed*:
   every door, trapdoor and gate closed, no gaps. Right-click it to see whether the room is sealed and how many
   nights are left in it. Open the door at night and the ward is gone until you shut it.
   Behind a sealed ward you can sleep; anywhere else you cannot.
4. **Each dawn costs the talisman a night.** You start with one that holds three. A crafted one holds seven.
5. **The Hollow get worse.** Every three nights survived they step up a stage, to a top stage of four:

   | Stage | Nights | Around you | Speed | Damage | Through a cobblestone wall | Can break |
   |---|---|---|---|---|---|---|
   | 0 | 0-2 | 3 | slow walk | 11 | 6 s per block | up to planks, stone, wooden doors |
   | 1 | 3-5 | 5 | walk | 13 | 4 s | + iron doors' weaker cousins (hardness 4) |
   | 2 | 6-8 | 7 | brisk | 15 | 3 s | + iron doors, iron bars (hardness 5) |
   | 3 | 9-11 | 9 | jog | 17 | 2 s | hardness 6 |
   | 4 | 12+ | 11 | near a player's run | 19 | 2 s | hardness 7 |

   Obsidian and a sealed ward stop them at every stage. They also spawn closer in at each stage.
6. **The mist is a place that travels.** Every day or two a bank about 200 blocks across forms roughly 300 blocks
   from a player and drifts across the world for most of a day. A horn sounds and you are told which direction it
   is coming from; within 200 blocks of its edge you get a distance and bearing every ten seconds. It is aimed
   loosely at where you were, so it may roll over your base, clip it, or miss. You can walk out of its path,
   wait it out indoors (no sleeping under it), or go in.
7. **In the mist:** Skitterers that hunt in packs, Stingers that come to light and will go through a lit window,
   and leviathans crossing overhead that kill whatever is under a foot. None of them can leave the bank; they
   dissolve in clear air. There are two leviathans, and you feel both through the ground before you see them:
   - **Strider**: spider-legged, about 15 blocks to the belly, quick for its size.
   - **Colossus** (one in three): about 30 blocks to the belly on six pillar legs, a curtain of feelers hanging
     from its front, a foghorn call, and Stingers travelling with it. Its footfall is lethal in a 4-block radius.
     Following one is the fastest way to farm essence and the most likely way to die.
8. **Mist kills pay.** Every Skitterer and Stinger drops mist essence and 15 XP, and a kill by a player has a 70%
   chance of a bonus: iron, gold, emeralds, diamonds, bottles o' enchanting, or rarely netherite scrap.

## Your character

Press **K** for the character screen.

- **Level** (to 30). Experience comes from living through a night (100, more at later Hollow stages), killing mist
  creatures (30 each), other monsters (5) and skill level-ups (15). Each level is one talent point.
- **Talents.** Three trees of five. Each talent needs a rank in the one above it; the last in each tree is an
  active ability that also needs 6 points spent in that tree.

  | Warden (the night) | Hunter (the mist) | Survivor |
  |---|---|---|
  | Steady Hands 3: essence feeds a talisman +1 night per rank | Mist Eyes 3: see 25% further in mist per rank | Vigor 3: +1 heart per rank |
  | Quiet Step 3: Hollow sense you 15% less far per rank | Cull 3: +10% damage to mist creatures per rank | Fleet 3: +4% speed per rank |
  | Thick Skin 3: -8% damage from Hollow per rank | Harvester 3: 15% per rank for an extra essence | Quick Study 3: +10% all experience per rank |
  | Keeper 2: 15% per rank a nearby talisman loses nothing at dawn | Antivenom 2: stinger poison halved, then immune | Tough 2: +2 armour per rank |
  | **Ward Pulse** (Z): throw back and slow every Hollow within 12 blocks | **Clear Air** (X): thin the mist and mark its creatures for 20 s | **Adrenaline** (C): Speed II and Resistance for 8 s |

- **Skills** rise by use, to level 25: Combat (+2% melee damage per level, from melee damage dealt), Archery
  (+2% projectile damage, from projectile damage dealt), Mining (+2% digging speed, from blocks broken),
  Athletics (+0.6% speed, from sprinting), Endurance (-1% damage taken, from damage taken).

Progress is saved with the world, per player, and kept through death. Every number is in `rpg/Rpg.java`.

## Dying

- **Standard world:** you respawn with what was on you: hotbar, armour and off hand. Everything in your pack
  stays where you fell as a glowing pile that never despawns, and you are told the coordinates. Level, talents
  and skills are kept.
- **Hardcore world** (Minecraft's own Hardcore option when you create the world): one life. When you die the
  mod announces how many nights the run lasted and at what level.
- With the `keepInventory` game rule on, vanilla keeps everything and the mod stands aside.

## What mist essence buys

| Use | Cost |
|---|---|
| Recharge a talisman (+3 nights) | 1 essence, right-click |
| New talisman (7 nights) | string on top, stone / essence / stone, stone below |
| Mending book | 4 essence + a book, any arrangement |
| Enchanted golden apple | 8 essence around a golden apple |
| Totem of undying | essence, emerald, essence / essence, gold block, essence / essence in the bottom middle |

Deep underground (about 14 blocks below the surface) nothing spawns on you, but the Hollow dig.

## Commands (operators)

| Command | Does |
|---|---|
| `/mist start` | drops a bank on you |
| `/mist approach` | forms one 300 blocks away, as the game does |
| `/mist stop` | thins the current bank out |
| `/mist status` | where the bank is, nights survived, Hollow stage |
| `/mist nights <n>` | sets nights survived, to try a later Hollow stage |
| `/mist xp <n>` | gives you character experience |
| `/mist leviathan` | sends a Strider past you (needs mist) |
| `/mist colossus` | sends a Colossus past you (needs mist) |

## Building

Needs a Java 25 JDK. From this folder:

```
gradlew build                 # jar lands in build/libs/
gradlew runClient             # dev client
gradlew runClientGameTest     # boots the game, checks the rules, saves screenshots to build/run/clientGameTest/screenshots
```

Licence: MIT.

Art is generated placeholder: `python tools/make_textures.py` rewrites every PNG, `python tools/make_assets.py`
the JSON (language, recipes, loot). Repaint the PNGs in `src/main/resources/assets/fromthemist/textures` freely;
keep their sizes. 

Sounds: `python tools/make_sounds.py` builds every sound file and `sounds.json` (run it before `make_assets.py`,
which reads the subtitles it writes). Thirteen are synthesized in that script; twelve are CC0 recordings from
freesound.org kept in `tools/sounds_raw/`. Sources are listed in `SOUND-CREDITS.md`. A few vanilla sounds remain
(the Hollow breaking blocks, the mist thinning).

Tuning: `MistDirector` (bank distance and drift, spawn counts), `MistState` (bank size, nights per stage),
`HollowEntity.customServerAiStep` (what each stage changes), `TalismanBlock` (nights per talisman),
`tools/make_assets.py` (loot weights and recipes).

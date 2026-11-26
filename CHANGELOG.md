# Changelog

Unofficial community port of Mariculture by joshiejack.

## 0.8.0

- Added Thaumaturge compat on 1.21.1, 26.1.2 and 26.2, loaded only when Thaumaturge is installed
- Added Thaumaturge aspects for every Mariculture item and block
- Added six magic fish, bred in a tank of Brine of Order
- Added molten thaumium, molten void and Brine of Order fluids
- Added Thaumaturge crucible and infusion recipes and Thaumaturge rows for the vat, sifter, crucible furnace, casters and fish feeder
- Added the Aquamancy research tab
- Added Auramfin putting vis into the aura and Flux Lamprey eating flux
- Added the warded diving set and the revealing scuba mask
- Added fish breeding that needs the tank owner's research
- Added Minecraft 26.2 support
- Changed Patchouli to optional on 26.2, where the Compendium needs it installed

## 0.7.0.2

- Fixed the mod asking for a Fundamentum version that is not out yet

## 0.7.0.1

- Added Wooden and Titanium fishing rod recipes
- Added colored slot borders, faint item hints and tooltips to machine menus
- Added a Compendium page on how Mariculture differs from the original
- Changed shift-click in machine menus to put items in the slot made for them
- Changed keybinds to their own Controls category
- Changed the Compendium to show in the creative tab and JEI, and to be given once on first join
- Changed Fundamentum to required, version 2.0.0 or newer
- Changed the limestone sea bed to also form under dirt and mud floors, set by `mariculture:seabed_floor`
- Fixed the bobber never dipping on Mariculture rods
- Fixed rod catches sinking in water
- Fixed the broken Metal Fish page in the Compendium
- Fixed DNA machines losing progress without power or when a new battery goes in
- Fixed water flowing into machines
- Fixed rods from other mods showing up when that mod is missing

## 0.7.0

Fish for Alex's Caves. They load only with Alex's Caves installed, and everything about them is in data files that datapacks can change.

### Added
- 14 new fish live in the cave biomes of Alex's Caves and bite on Mariculture rods. They breed in a Fish Feeder and give fish oil, meal and their own products like any other Mariculture fish.
- Toxic Caves has the Acid Lamprey. Magnetic Caves has the Lodestone Ray and Ferrofish, and Primordial Caves has the Old Lungfish and Amberfin.
- The Abyssal Chasm has the Gulper and Lanternjaw, and Forlorn Hollows has the Gloomfish and Ghost Minnow.
- Candy Cavity has the Taffy Tang, Licorice Eel, Peppermint Pike, Chocolate Catfish and Gumdrop Goldfish.
- Mariculture hooks float on Toxic Caves acid and Candy Cavity purple soda. The Acid Lamprey bites only in acid, and the candy fish bite only in soda.
- Species files take `catch.biomes` and `catch.fluid`, so datapacks can tie any fish to a biome or a fluid.
- A guide book page lists the cave fish.
- Metallurgy: Recast ores have metal fish that give droplets of the molten metal.
- Each metal fish has an effect that matches its metal, and a pair breeds only when its needs hold, like odd foods, light, depth or tank size.

## 0.6.0

Compatibility with other fishing and ocean mods. Everything new here is in data files that datapacks can change.

### Added
- Raw fish from any mod in the `mariculture:foreign_fish` tag make fish meal and sushi. The tag already holds the fish from Aquaculture 2, Upgrade Aquatic, Alex's Caves, Starcatcher and Tide.
- Biome salinity and temperature now come from datapack files in `mariculture/biome_environment`. Modded oceans count as salt water and modded beaches as brackish water. Alex's Caves' Abyssal Chasm is cold salt water.
- Rods from other mods can fish Mariculture's catch at a set tier, listed in `mariculture/rod_tiers`. Aquaculture's iron, gold, diamond and neptunium rods are in that list.
- Mariculture fish show up in Starcatcher's pools and guide.
- On 1.20.1 and 1.21.1, Mariculture fish bite on Tide rods and fill pages in Tide's journal. Mariculture rods keep Mariculture fishing.
- The Stardew Fishing minigame now plays for the Mariculture fish on the hook.
- Upgrade Aquatic kelps work in kelp wraps. Alex's Caves pearls count as pearls.

### Fixed
- Fixed a crash when reeling in a Tide rod with Mariculture installed.

## 0.5.0

### Changed
- **Requires Curios.** Rings, bracelets and necklaces are worn in Curios' ring, bracelet and necklace slots. With
  extra slots from other mods or data packs, the strongest piece of each type counts (two rings of Fall do not add
  up); `jewelry.extra_slots_stack` makes every slot count. The old built-in jewelry slots are gone; jewelry that was
  worn there in an older world is not carried over.
- Jewelry stays on the player when they die (as before). With `jewelry.drops_on_death` on, it drops, unless a
  Reaper piece keeps the inventory.
- The Mirror's three wear slots show and edit the first Curios ring, bracelet and necklace slot.
- The Mirror's texture matches the Magic and Celestial Mirrors: a round hand mirror with an iron rim.
- The creative tab is grouped: fishing, fish, food, pearls and jewelry, diving gear, machines and upgrades,
  materials, building blocks, then buckets.
- NeoForge 1.21.1 and 26.1.2: item data (fish genes, eggs, jewelry, worked items, stored energy) moved from custom
  data into named components (`mariculture:fish`, `mariculture:eggs`, `mariculture:jewelry`, `mariculture:worked`,
  `mariculture:energy`, `mariculture:item_data`). Items from older worlds are read as before and move over the next
  time they change. Forge 1.20.1 keeps item NBT.

### Added
- Advancements: a Mariculture tab with 16 goals, from the first rod and bait to pearls, jewelry, the Celestial
  Mirror, the crucible, titanium and scuba gear.
- Fluid tags: every molten metal in `c:molten_<name>`, fish oil in `c:fish_oil`, liquid experience in
  `c:experience` (`forge:` on 1.20.1).

### Fixed
- NeoForge 1.21.1 and 26.1.2: the fishing rods took no Lure, Luck of the Sea, Unbreaking or Mending from the
  enchanting table or an anvil. They are also in `c:tools/fishing_rod` now.
- Mariculture's fluid buckets are in `c:buckets`, so other mods' bucket recipes and filters see them.

## 0.4.1 (first public release)

### Changed
- Texture overhaul: 340 block, item and armour textures redone in a vanilla-matched style; the originals are kept
  where they already fit (rods, pearls, jewelry, upgrades).

## 0.4.0 (not released)

### Added
- Data-pack tweaks (`data/<ns>/mariculture/tweaks/*.json`) and a KubeJS binding (`Mariculture.add` in server
  scripts) on all three editions, with the same changes as the CraftTweaker handlers (`docs/tweaks.md`).
- Fishing speed: the legacy "Give me fish NAOW" multiplier is `fishery.fishing_speed_multiplier` again (default 50),
  and data packs can override it or speed up single rods (`mariculture/fishing_speed`).
- JEI fishing pages: fishing loot by rarity and rod, bait and the rods that take it, where and how each species is
  caught, its breeding mutations and what it gives.
- Hephaestus (NeoForge 1.21.1 and 26.1.2), Tinkers' successor: titanium melts into Hephaestus' molten titanium,
  rutile and magnesium melt and alloy, limestone melts into quicklime, titanium casts every Hephaestus Tools part,
  and the optional smeltery recipe import into the crucible and casters.
- Neo Vitae (NeoForge 1.21.1 and 26.1.2), Blood Magic's successor: the bound fishing rod is bound in its binding
  array, takes its weak blood shard, catches its loot and is repaired with a tabula rasa.
- Forestry: the aquatic backpack and its woven version (carpenter), fish oil as biogas engine fuel and kelp as
  fermenter fuel (Forge 1.20.1, NeoForge 1.21.1).
- Complicated Bees (all three editions): its drones, princesses and queens craft into 1, 5 and 7 bee bait.

### Fixed
- NeoForge 26.1.2 crashed while loading with Jade installed: Jade 26.1 rejects a provider that is both a data and a
  tooltip provider (BUG-073).
- NeoForge 1.21.1: Forestry 3.0 rejected every Mariculture squeezer recipe, which lacked the id it requires (BUG-074).

## 0.3.0

### Added
- CraftTweaker support (Forge 1.20.1, NeoForge 1.21.1; CraftTweaker has no 26.1 release) replacing the legacy
  MineTweaker3 plugin: vat, crucible melting and fuels, anvil, casting and fishing loot can be added or removed
  from ZenScript (`docs/crafttweaker.md`).
- JEI jewelry category: every ring, bracelet and necklace the grid can make, with its durability and enchantment limits.
- Tinkers' Construct (Forge 1.20.1): titanium as a tool material, and the optional smeltery recipe import into
  the crucible and casters (config `plugins.tconstruct_recipe_import`).
- Lava fishing: in the Nether, Mariculture rods fish in lava; the hook and the catch are fire-immune.
- Debug upgrades (creative only): killer, always-live, hatching, speed, heating, storage and machine upgrades.

### Changed
- The thaumic-compat branch (0.2.0) is merged into main.

## 0.2.0 (branch thaumic-compat, merged)

### Changed
- Credits: the port is credited to Oculifelis (mods.toml authors, LICENSE, README); joshiejack stays as the
  original author (MIT notice).

### Added
- 73 flat legacy textures (droplets, nuggets, dusts, lit neon lamps, tanks, nets, rods, diving and scuba items)
  redrawn with shading by `tools/enrich_textures.py`; silhouettes unchanged.
- Thaumaturge compat (NeoForge 26.1.2): the legacy Thaumcraft plugin's item aspects on Mariculture metals,
  limestone, oysters, pearls, bait, droplets and coral, as data loaded only when Thaumaturge is present.
- Platform-neutral core-domain with the content catalog, a resource exporter for all three editions,
  and unit tests.
- Forge 1.20.1, NeoForge 1.21.1 and NeoForge 26.1.2 targets (written, not yet compiled).
- Limestone family, coral rock, bauxite and rutile ores, metal blocks, pearls, pearl blocks and bricks.
- Bauxite ore generation and the limestone sea bed with rutile ore.
- Pearl oysters on ocean floors; fill them with sand to grow pearls.
- Patchouli book `mariculture:compendium` replacing Enchiridion (2 entries so far).
- Fish genetics rules (species and mutations as data packs), not yet wired to items.

### Fixed (relative to Mariculture 1.3.0)
- Zombies got fishing gear about 11% of the time instead of the configured 1.25% (BUG-001).
- Spent eggs could hatch forever (BUG-031); species ids could shift between launches (BUG-025).
- Sea bed: per-biome limestone depths were ignored (BUG-041); rutile and sea bed noise were not
  reproducible from the world seed (BUG-036, BUG-042); generation is opt-in by biome tag (BUG-009).
- Oysters were all created in one 4x4 patch outside the chunk being decorated (BUG-051).

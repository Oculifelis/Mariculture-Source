package mariculture.domain.content;

import java.util.List;
import java.util.Map;

import mariculture.domain.worldgen.Coral;

/**
 * World Plus blocks (world/WorldPlus.java, world/BlockCoral.java, 1.3.0): dried and living coral in eleven colours.
 * Dried coral crushes into one dye by hand (two on the anvil, see {@link mariculture.domain.metal.Anvil}); the vat
 * bleaches it with quicklime and brings it to life with water ({@link mariculture.domain.metal.VatRecipes}).
 *
 * <p>Changes: legacy kelp is vanilla kelp (WORLD-001), so its blocks and the kelp seeds are not here.
 */
final class WorldContent {
    private WorldContent() {}

    /** Legacy texture names (textures/blocks/coral_*.png) by colour. */
    private static final Map<Coral.Colour, String> TEXTURE = Map.ofEntries(
            Map.entry(Coral.Colour.PINK, "pink"), Map.entry(Coral.Colour.RED, "red"), Map.entry(Coral.Colour.LIGHT_BLUE, "light_blue"),
            Map.entry(Coral.Colour.MAGENTA, "magenta"), Map.entry(Coral.Colour.BROWN, "brown"), Map.entry(Coral.Colour.YELLOW, "yellow"),
            Map.entry(Coral.Colour.ORANGE, "orange"), Map.entry(Coral.Colour.PURPLE, "purple"), Map.entry(Coral.Colour.GREY, "grey"),
            Map.entry(Coral.Colour.LIGHT_GREY, "light_grey"), Map.entry(Coral.Colour.WHITE, "white"));

    static void register() {
        int meta = 2;
        for (Coral.Colour c : Coral.Colour.values()) {
            Map<String, String> tex = Map.of("all", "coral/" + TEXTURE.get(c));
            // BlockCoral.getBlockHardness: 1 for coral; no resistance was set.
            Catalog.add(new BlockSpec(c.dried(), "Dried " + c.name, "coral:" + meta, Shape.CORAL, Sound.CORAL, 1f, 0f, false, HarvestLevel.ANY,
                    tex, true));
            Catalog.add(new BlockSpec(c.living(), c.name, "coralGrowable:" + meta, Shape.LIVING_CORAL, Sound.CORAL, 1f, 0f, false,
                    HarvestLevel.ANY, tex, true));
            Catalog.itemTag("mariculture:coral", c.dried());
            Catalog.itemTag("mariculture:coral", c.living());
            Catalog.blockTag("mariculture:living_coral", c.living());
            // WorldPlus.java:87-97 addCrushRecipe(dye, coral, false): one dye from the dried coral by hand.
            Catalog.RECIPES.add(new Recipe.Shapeless(c.id() + "_dye_from_coral", List.of(Ingredient.item(c.dried())), c.dye, 1,
                    "WorldPlus.java:87"));
            meta++;
        }
        // BlockCoral.canSustainCoral: Mariculture sands, limestone, sand, cobblestone, mossy cobblestone, coral rock.
        for (String g : List.of("minecraft:sand", "#mariculture:limestone", "minecraft:cobblestone", "minecraft:mossy_cobblestone",
                "coral_rock", "ancient_sand", "bubbling_sand")) {
            Catalog.blockTag("mariculture:coral_ground", g);
        }
        // Natural gas (core/blocks/BlockAir meta 0): the block gas pockets (Catalog ORES, natural_gas_pocket) are made of.
        Catalog.add(new BlockSpec(mariculture.domain.fluid.NaturalGas.BLOCK, "Natural Gas", "air:0", Shape.GAS, Sound.GLASS, 0f, 0f, false,
                HarvestLevel.ANY, Map.of("all", "natural_gas"), false));
        // Rockhopper (EntityRockhopper.isBreedingItem: vanilla and Mariculture fish; egg WorldPlus.java:76).
        Catalog.ITEMS.add(new ItemSpec("rockhopper_spawn_egg", "Rockhopper Spawn Egg", "rockhopperEgg", "rockhopper_spawn_egg", 64,
                new ItemKind.Special("spawn_egg", 0)));
        for (String f : List.of("#minecraft:fishes", "#mariculture:raw_fish")) Catalog.itemTag("mariculture:rockhopper_food", f);
        // Legacy animals spawned on grass only, which beaches rarely have; here the beach blocks themselves.
        for (String b : List.of("minecraft:sand", "minecraft:gravel", "minecraft:snow_block", "minecraft:grass_block", "minecraft:stone")) {
            Catalog.blockTag("mariculture:rockhopper_spawnable_on", b);
        }
    }
}

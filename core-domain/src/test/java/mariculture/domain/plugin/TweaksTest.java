package mariculture.domain.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import mariculture.domain.fish.Species;
import mariculture.domain.fishing.FishingRules;
import mariculture.domain.fluid.Fluids;
import mariculture.domain.machine.FluidAmount;
import mariculture.domain.machine.Stack;
import mariculture.domain.metal.Anvil;
import mariculture.domain.metal.Casting;
import mariculture.domain.metal.CrucibleFuels;
import mariculture.domain.metal.Melting;
import mariculture.domain.metal.VatRecipes;

class TweaksTest {
    private static final java.util.function.Predicate<String> NO_TAGS = t -> false;

    @AfterEach
    void reset() {
        Tweaks.clear();
    }

    @Test
    void vatAddWinsAndRemoveHidesByOutput() {
        assertEquals("obsidian", VatRecipes.find(Fluids.WATER, 1000, Fluids.LAVA, 1000, Stack.EMPTY, NO_TAGS).id());
        Tweaks.VatRemove remove = new Tweaks.VatRemove("minecraft:obsidian", null);
        Tweaks.apply(remove);
        assertNull(VatRecipes.find(Fluids.WATER, 1000, Fluids.LAVA, 1000, Stack.EMPTY, NO_TAGS));
        Tweaks.VatAdd add = new Tweaks.VatAdd(new VatRecipes.Recipe("script_0", new FluidAmount(Fluids.WATER, 500),
                new FluidAmount(Fluids.LAVA, 500), null, 0, null, Stack.of("minecraft:stone"), 2, "script"));
        Tweaks.apply(add);
        assertEquals("script_0", VatRecipes.find(Fluids.WATER, 1000, Fluids.LAVA, 1000, Stack.EMPTY, NO_TAGS).id());
        Tweaks.undo(add);
        Tweaks.undo(remove);
        assertEquals("obsidian", VatRecipes.find(Fluids.WATER, 1000, Fluids.LAVA, 1000, Stack.EMPTY, NO_TAGS).id());
    }

    @Test
    void crucibleRecipesAndFuels() {
        assertNotNull(Melting.find("minecraft:bucket", 2000, NO_TAGS, null));
        Tweaks.apply(new Tweaks.MeltRemove("minecraft:bucket"));
        assertNull(Melting.find("minecraft:bucket", 2000, NO_TAGS, null));
        Tweaks.apply(new Tweaks.MeltAdd(new Melting.Recipe("minecraft:diamond", 900, "mariculture:molten_iron", 10, null, 0, List.of(), List.of())));
        assertEquals(10, Melting.find("minecraft:diamond", 1000, NO_TAGS, null).amount());
        assertNull(Melting.find("minecraft:diamond", 800, NO_TAGS, null));

        assertNotNull(CrucibleFuels.item("minecraft:coal", NO_TAGS));
        Tweaks.apply(new Tweaks.FuelRemove("minecraft:coal", false));
        assertNull(CrucibleFuels.item("minecraft:coal", NO_TAGS));
        Tweaks.apply(new Tweaks.FuelAdd("lava", true, new CrucibleFuels.Fuel(2000, 99, 10, 0, 0)));
        assertEquals(99, CrucibleFuels.fluid("minecraft:lava").maxTempPer());
        assertNotNull(CrucibleFuels.fluid("mariculture:natural_gas"));
        Tweaks.apply(new Tweaks.FuelRemove("natural_gas", true));
        assertNull(CrucibleFuels.fluid("mariculture:natural_gas"));
    }

    @Test
    void anvilAndCasting() {
        assertEquals("minecraft:bone_meal", Anvil.recipe("minecraft:bone").output());
        Tweaks.apply(new Tweaks.AnvilAdd(new Anvil.Recipe("minecraft:bone", "minecraft:white_dye", 3, 7)));
        assertEquals("minecraft:white_dye", Anvil.recipe("minecraft:bone").output());
        assertEquals(1, Anvil.recipes().stream().filter(r -> r.input().equals("minecraft:bone")).count());
        Tweaks.apply(new Tweaks.AnvilRemove("minecraft:kelp"));
        assertNull(Anvil.recipe("minecraft:kelp"));
        assertTrue(Anvil.recipes().stream().noneMatch(r -> r.input().equals("minecraft:kelp")));

        assertEquals("minecraft:iron_ingot", Casting.recipeFor(Casting.Kind.INGOT, "mariculture:molten_iron").item());
        Tweaks.apply(new Tweaks.CastRemove(Casting.Kind.INGOT, "minecraft:iron_ingot"));
        assertNull(Casting.recipeFor(Casting.Kind.INGOT, "mariculture:molten_iron"));
        assertNotNull(Casting.recipeFor(Casting.Kind.NUGGET, "mariculture:molten_iron"));
        Tweaks.apply(new Tweaks.CastAdd(Casting.Kind.BLOCK, "minecraft:lava", "minecraft:magma_block", 500));
        assertEquals(new Casting.Result("minecraft:magma_block", 500), Casting.result(Casting.Kind.BLOCK, "minecraft:lava", 600, t -> null));
        assertNull(Casting.result(Casting.Kind.BLOCK, "minecraft:lava", 400, t -> null));
    }

    @Test
    void fishingLoot() {
        List<FishingRules.Loot> base = List.of(new FishingRules.Loot("minecraft:ender_pearl", 15, FishingRules.Rarity.JUNK, "end", Species.Rod.OLD));
        Tweaks.apply(new Tweaks.LootRemove("minecraft:ender_pearl"));
        assertTrue(Tweaks.loot(FishingRules.Rarity.JUNK, Species.Rod.DIRE, base).isEmpty());
        Tweaks.apply(new Tweaks.LootAdd(new FishingRules.Loot("minecraft:diamond", 5, FishingRules.Rarity.RARE, null, Species.Rod.GOOD), true));
        assertTrue(Tweaks.loot(FishingRules.Rarity.RARE, Species.Rod.SUPER, List.of()).isEmpty());
        assertEquals(1, Tweaks.loot(FishingRules.Rarity.RARE, Species.Rod.GOOD, List.of()).size());
        assertFalse(Tweaks.loot(FishingRules.Rarity.JUNK, Species.Rod.GOOD, List.of()).stream().anyMatch(l -> l.item().equals("minecraft:diamond")));
    }

    @Test
    void importsComeAfterTheBuiltInTables() {
        Tweaks.setImports(List.of(
                new Tweaks.MeltAdd(new Melting.Recipe("minecraft:iron_ingot", 1538, "tconstruct:molten_iron", 90, null, 0, List.of(), List.of())),
                new Tweaks.MeltAdd(new Melting.Recipe("tconstruct:cobalt_ingot", Plugins.tinkersToCrucible(950), "tconstruct:molten_cobalt", 90, null, 0,
                        List.of(), List.of())),
                new Tweaks.CastAdd(Casting.Kind.INGOT, "tconstruct:molten_cobalt", "tconstruct:cobalt_ingot", 90),
                new Tweaks.CastAdd(Casting.Kind.INGOT, "mariculture:molten_iron", "tconstruct:cobalt_ingot", 90)));
        assertEquals("mariculture:molten_iron", Melting.find("minecraft:iron_ingot", 2000, t -> t.equals("c:ingots/iron"), null).fluid());
        assertEquals(1826, Melting.find("tconstruct:cobalt_ingot", 2000, NO_TAGS, null).temp());
        assertEquals("tconstruct:cobalt_ingot", Casting.recipeFor(Casting.Kind.INGOT, "tconstruct:molten_cobalt").item());
        assertEquals("minecraft:iron_ingot", Casting.recipeFor(Casting.Kind.INGOT, "mariculture:molten_iron").item());
        assertEquals(1538, Plugins.tinkersToCrucible(800));
        Tweaks.setImports(List.of());
        assertNull(Melting.find("tconstruct:cobalt_ingot", 2000, NO_TAGS, null));
    }
}

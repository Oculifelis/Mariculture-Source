package mariculture.domain.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import mariculture.domain.fish.Species;
import mariculture.domain.fishing.FishingRules;
import mariculture.domain.fishing.FishingSpeed;
import mariculture.domain.fishing.LavaBite;
import mariculture.domain.fluid.Fluids;
import mariculture.domain.machine.Stack;
import mariculture.domain.metal.Anvil;
import mariculture.domain.metal.Casting;
import mariculture.domain.metal.Melting;
import mariculture.domain.metal.VatRecipes;
import mariculture.domain.random.Rng;

/** Data-pack tweaks (mariculture/tweaks) and the fishing speed data (mariculture/fishing_speed). */
class TweakDataTest {
    private static final java.util.function.Predicate<String> NO_TAGS = t -> false;

    @AfterEach
    void reset() {
        Tweaks.clear();
        FishingSpeed.setData(List.of());
    }

    @Test
    void everyChangeTypeParsesAndApplies() {
        List<Tweaks.Change> changes = TweakData.parse("pack:test", """
                {"changes": [
                  {"type": "vat_remove", "output_item": "minecraft:obsidian"},
                  {"type": "vat_add", "fluid1": {"fluid": "minecraft:water", "amount": 500}, "fluid2": {"fluid": "minecraft:lava", "amount": 500},
                   "output_item": {"item": "minecraft:stone", "count": 2}, "time": 3},
                  {"type": "melting_remove", "input": "minecraft:iron_ingot"},
                  {"type": "melting_add", "input": "minecraft:stick", "temperature": 300, "fluid": "minecraft:water", "amount": 10},
                  {"type": "fuel_add", "tag": "c:rods/wooden", "max_temperature": 500, "temperature_per_tick": 2, "ticks_per_step": 4},
                  {"type": "anvil_add", "input": "minecraft:cobblestone", "output": "minecraft:gravel", "hits": 2},
                  {"type": "casting_add", "caster": "ingot", "fluid": "minecraft:water", "result": "minecraft:ice", "amount": 1000},
                  {"type": "loot_add", "item": "minecraft:diamond", "chance": 50, "rarity": "rare", "dimension": "end", "rod": "super"},
                  {"type": "loot_remove", "item": "minecraft:nether_star"}
                ]}""", "test");
        assertEquals(9, changes.size());
        Tweaks.setData(changes);
        VatRecipes.Recipe vat = VatRecipes.find(Fluids.WATER, 1000, Fluids.LAVA, 1000, Stack.EMPTY, NO_TAGS);
        assertEquals("pack:test/1", vat.id());
        assertTrue(vat.outItem().is("minecraft:stone"));
        assertNull(Melting.find("minecraft:iron_ingot", Integer.MAX_VALUE, NO_TAGS, null));
        assertEquals(300, Melting.find("minecraft:stick", Integer.MAX_VALUE, NO_TAGS, null).temp());
        assertEquals(500, Tweaks.itemFuel("minecraft:stick", t -> t.equals("c:rods/wooden"), () -> null).maxTemp());
        assertEquals("minecraft:gravel", Anvil.recipe("minecraft:cobblestone").output());
        assertEquals("minecraft:ice", Casting.recipeFor(Casting.Kind.INGOT, Fluids.WATER).item());
        List<FishingRules.Loot> rare = FishingRules.lootFor(Species.Rod.SUPER);
        assertTrue(rare.stream().anyMatch(l -> l.item().equals("minecraft:diamond") && "end".equals(l.dimension())));
        assertTrue(rare.stream().noneMatch(l -> l.item().equals("minecraft:nether_star")));
        // A reload replaces the data-pack changes as a whole.
        Tweaks.setData(List.of());
        assertEquals("obsidian", VatRecipes.find(Fluids.WATER, 1000, Fluids.LAVA, 1000, Stack.EMPTY, NO_TAGS).id());
    }

    @Test
    void scriptChangesWinOverDataPackOnes() {
        Tweaks.setData(TweakData.parse("pack:a", "{\"type\": \"anvil_add\", \"input\": \"minecraft:cobblestone\", \"output\": \"minecraft:gravel\", \"hits\": 2}", "a"));
        Tweaks.apply(new Tweaks.AnvilAdd(new Anvil.Recipe("minecraft:cobblestone", "minecraft:sand", 1, 1)));
        assertEquals("minecraft:sand", Anvil.recipe("minecraft:cobblestone").output());
    }

    @Test
    void badFilesSayWhatIsWrong() {
        assertThrows(IllegalArgumentException.class, () -> TweakData.parse("p:x", "{\"type\": \"vat_remove\"}", "x"));
        assertThrows(IllegalArgumentException.class, () -> TweakData.parse("p:x", "{\"type\": \"nope\"}", "x"));
        assertThrows(IllegalArgumentException.class, () -> TweakData.parse("p:x",
                "{\"type\": \"loot_add\", \"item\": \"a:b\", \"chance\": 1, \"rarity\": \"rare\", \"dimension\": \"moon\"}", "x"));
    }

    @Test
    void fishingSpeedComesFromConfigDataAndRods() {
        assertEquals(50, FishingSpeed.forRod("mariculture:reed_fishing_rod"));
        FishingSpeed.setData(List.of(FishingSpeed.parse("{\"speed_multiplier\": 0}", "a"),
                FishingSpeed.parse("{\"rods\": {\"mariculture:titanium_fishing_rod\": 12}}", "b"),
                FishingSpeed.parse("{\"rods\": {\"mariculture:titanium_fishing_rod\": 3}}", "c")));
        assertEquals(0, FishingSpeed.forRod("mariculture:reed_fishing_rod"));
        assertEquals(15, FishingSpeed.forRod("mariculture:titanium_fishing_rod"));
    }

    @Test
    void speedShortensTheWait() {
        Rng low = new Rng() {
            @Override
            public float nextFloat() {
                return 0.9f;
            }

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
        LavaBite bite = new LavaBite(0, 49);
        int ticks = 0;
        while (bite.tick(low, false) == LavaBite.Event.LURING) ticks++;
        // 1 tick to roll the 100-tick wait, then 50 a tick: 2 more ticks.
        assertEquals(3, ticks);
    }

    @Test
    void requiresSkipsFilesForMissingMods() {
        String json = "{\"requires\": [\"othermod\"], \"type\": \"anvil_add\", \"input\": \"minecraft:dirt\", \"output\": \"minecraft:sand\", \"hits\": 1}";
        try {
            Plugins.setModLoaded(id -> false);
            assertTrue(TweakData.parse("p:r", json, "r").isEmpty());
            Plugins.setModLoaded(id -> id.equals("othermod"));
            assertEquals(1, TweakData.parse("p:r", json, "r").size());
        } finally {
            Plugins.setModLoaded(id -> false);
        }
    }
}

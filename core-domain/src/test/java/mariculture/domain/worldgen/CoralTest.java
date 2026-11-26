package mariculture.domain.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import mariculture.domain.content.Catalog;
import mariculture.domain.metal.Anvil;
import mariculture.domain.metal.VatRecipes;
import mariculture.domain.random.ScriptedRng;
import mariculture.domain.worldgen.Coral.Colour;
import org.junit.jupiter.api.Test;

class CoralTest {
    @Test
    void elevenColoursEachWithDriedAndLivingBlocks() {
        assertEquals(11, Colour.values().length);
        for (Colour c : Colour.values()) {
            assertTrue(Catalog.blocks().stream().anyMatch(b -> b.id().equals(c.dried())), c.dried());
            assertTrue(Catalog.blocks().stream().anyMatch(b -> b.id().equals(c.living())), c.living());
        }
        assertEquals("live_light_blue_coral", Colour.LIGHT_BLUE.living());
    }

    @Test
    void reefsPlaceTheEightColouredCoralsOnlyAndASpongeOneInTwentyFive() {
        assertEquals(Colour.PINK, Coral.reefColour(new ScriptedRng().ints(0)));
        assertEquals(Colour.PURPLE, Coral.reefColour(new ScriptedRng().ints(7)));
        assertTrue(Coral.reefSponge(new ScriptedRng().ints(0)));
        assertFalse(Coral.reefSponge(new ScriptedRng().ints(24)));
    }

    @Test
    void colourMixingFollowsTheLegacyTable() {
        // Half the time the coral keeps its own colour, then half the other's, then a mix from the pair table.
        assertEquals(Colour.RED, Coral.newColour(Colour.RED, Colour.WHITE, new ScriptedRng().ints(0)));
        assertEquals(Colour.WHITE, Coral.newColour(Colour.RED, Colour.WHITE, new ScriptedRng().ints(1, 0)));
        assertEquals(Colour.YELLOW, Coral.newColour(Colour.RED, Colour.WHITE, new ScriptedRng().ints(1, 1, 1)));
        // The table is symmetric.
        assertEquals(Colour.PINK, Coral.newColour(Colour.WHITE, Colour.RED, new ScriptedRng().ints(1, 1, 0)));
    }

    @Test
    void spreadingNeedsCoralRockAndALivingNeighbour() {
        List<Colour> grown = new ArrayList<>();
        Coral.SpreadWorld world = new Coral.SpreadWorld() {
            public boolean onCoralRock() { return true; }
            public Colour livingAt(int dx, int dy, int dz) { return Colour.RED; }
            public boolean canGrowAt(int dx, int dy, int dz) { return true; }
            public void grow(int dx, int dy, int dz, Colour colour) { grown.add(colour); }
        };
        // Spread roll 0, offsets a (five draws), keep own colour, offsets b (five draws).
        Coral.spreadTick(Coral.Settings.LEGACY, Colour.RED, world, new ScriptedRng().ints(0, 1, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0));
        assertEquals(List.of(Colour.RED), grown);
        Coral.spreadTick(Coral.Settings.LEGACY, Colour.RED, world, new ScriptedRng().ints(3));
        assertEquals(1, grown.size(), "missed spread roll");
    }

    @Test
    void bleachingAndSoakingInTheVat() {
        VatRecipes.Recipe bleach = VatRecipes.all().stream().filter(r -> r.id().equals("bleach_pink_coral")).findFirst().orElseThrow();
        assertEquals("mariculture:quicklime", bleach.fluid1().fluid());
        assertEquals(50, bleach.fluid1().amount());
        assertEquals("mariculture:grey_coral", bleach.outItem().item());
        assertEquals(5, bleach.time());
        assertTrue(VatRecipes.all().stream().noneMatch(r -> r.id().equals("bleach_white_coral")));
        VatRecipes.Recipe soak = VatRecipes.all().stream().filter(r -> r.id().equals("soak_grey_coral")).findFirst().orElseThrow();
        assertEquals("mariculture:live_grey_coral", soak.outItem().item());
        assertEquals(100, soak.fluid1().amount());
        assertEquals(5, soak.time());
    }

    @Test
    void anvilCrushesCoralToTwoDyeAndKelpToGreen() {
        Anvil.Recipe r = Anvil.recipe("mariculture:light_blue_coral");
        assertNotNull(r);
        assertEquals("minecraft:light_blue_dye", r.output());
        assertEquals(2, r.count());
        assertEquals("minecraft:green_dye", Anvil.recipe("minecraft:kelp").output());
        assertNull(Anvil.recipe("mariculture:live_pink_coral"));
    }
}

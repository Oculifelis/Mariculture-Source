package mariculture.domain.worldgen;

import static mariculture.domain.worldgen.Seabed.Action.KEEP;
import static mariculture.domain.worldgen.Seabed.Action.LIMESTONE;
import static mariculture.domain.worldgen.Seabed.Action.RUTILE;
import static mariculture.domain.worldgen.Seabed.Action.SAND;
import static mariculture.domain.worldgen.Seabed.Cell.OTHER;
import static mariculture.domain.worldgen.Seabed.Cell.STONE;
import static mariculture.domain.worldgen.Seabed.Cell.SURFACE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mariculture.domain.random.ScriptedRng;
import mariculture.domain.worldgen.Seabed.Action;
import mariculture.domain.worldgen.Seabed.Cell;
import mariculture.domain.worldgen.Seabed.Kind;
import mariculture.domain.worldgen.Seabed.Settings;
import org.junit.jupiter.api.Test;

class SeabedTest {
    private static final Settings S = Settings.LEGACY;

    @Test
    void oceanColumnGetsLimestoneUnderTheSurfaceLayer() {
        ScriptedRng rng = new ScriptedRng().floats(0f).ints(1, 1, 1, 0, 1, 1, 1);
        Action[] plan = Seabed.plan(S, Kind.OCEAN, new Cell[] {SURFACE, SURFACE, STONE, STONE, STONE, STONE, STONE, STONE,
                STONE, STONE, STONE}, 40, 63, 0, rng);
        assertArrayEquals(new Action[] {KEEP, KEEP, LIMESTONE, LIMESTONE, LIMESTONE, RUTILE, LIMESTONE, LIMESTONE,
                LIMESTONE, KEEP, KEEP}, plan);
        assertTrue(rng.exhausted(), "one rutile draw per limestone block and no more");
    }

    @Test
    void bareStoneFloorIsCappedWithSandLikeLegacy() {
        ScriptedRng rng = new ScriptedRng().floats(0f).ints(1, 1);
        Action[] plan = Seabed.plan(S, Kind.RIVER, new Cell[] {STONE, STONE, STONE, STONE}, 40, 63, 0, rng);
        assertArrayEquals(new Action[] {SAND, LIMESTONE, LIMESTONE, KEEP}, plan);
    }

    @Test
    void shallowFloorsAreLeftAlone() {
        // thickness 7 at ocean depth 7.5: legacy only replaced floors below 61 - 7 = 54 with sea level 63
        Cell[] col = {SURFACE, STONE, STONE};
        assertEquals(KEEP, Seabed.plan(S, Kind.OCEAN, col, 54, 63, 0, new ScriptedRng().floats(0f))[1]);
        assertEquals(LIMESTONE, Seabed.plan(S, Kind.OCEAN, col, 53, 63, 0, new ScriptedRng().floats(0f).ints(1, 1))[1]);
    }

    @Test
    void layerStopsAtCavesAndOres() {
        Action[] plan = Seabed.plan(S, Kind.OCEAN, new Cell[] {SURFACE, STONE, OTHER, STONE}, 30, 63, 0,
                new ScriptedRng().floats(0f).ints(1));
        assertArrayEquals(new Action[] {KEEP, LIMESTONE, KEEP, KEEP}, plan);
    }

    @Test
    void thickSurfaceIsNotASeaBed() {
        Cell[] col = new Cell[12];
        java.util.Arrays.fill(col, SURFACE);
        col[11] = STONE;
        assertArrayEquals(new Action[12], fillKeep(Seabed.plan(S, Kind.OCEAN, col, 30, 63, 0,
                new ScriptedRng().floats(0f))));
    }

    @Test
    void bug041_eachBiomeUsesItsOwnDepth() {
        // legacy passed the ocean depth (7.5) to every biome; deep ocean is meant to be 3.5
        assertEquals(3, Seabed.thickness(0, S.depth(Kind.DEEP_OCEAN), new ScriptedRng().floats(0f)));
        assertEquals(2, Seabed.thickness(0, S.depth(Kind.RIVER), new ScriptedRng().floats(0f)));
        assertEquals(7, Seabed.thickness(0, S.depth(Kind.OCEAN), new ScriptedRng().floats(0f)));
        Settings flat = new Settings(true, false, 7.5, 3.5, 2.5, 25, true, 500);
        assertEquals(7.5, flat.depth(Kind.DEEP_OCEAN));
    }

    @Test
    void riversHaveTwiceTheRutile() {
        assertEquals(250, S.rutileOneIn(Kind.RIVER));
        assertEquals(500, S.rutileOneIn(Kind.OCEAN));
    }

    @Test
    void disabledOrNoRutileSettings() {
        Settings off = new Settings(false, true, 7.5, 3.5, 2.5, 25, true, 500);
        assertEquals(KEEP, Seabed.plan(off, Kind.OCEAN, new Cell[] {SURFACE, STONE}, 30, 63, 0, new ScriptedRng())[1]);
        Settings noRutile = new Settings(true, true, 7.5, 3.5, 2.5, 25, false, 500);
        ScriptedRng rng = new ScriptedRng().floats(0f);
        assertEquals(LIMESTONE, Seabed.plan(noRutile, Kind.OCEAN, new Cell[] {SURFACE, STONE}, 30, 63, 0, rng)[1]);
        assertTrue(rng.exhausted(), "no rutile draws when rutile is off");
        assertThrows(IllegalArgumentException.class, () -> new Settings(true, true, 0, 3.5, 2.5, 25, true, 500));
        assertThrows(IllegalArgumentException.class, () -> new Settings(true, true, 7.5, 3.5, 2.5, 25, true, 1));
    }

    private static Action[] fillKeep(Action[] plan) {
        Action[] expected = new Action[plan.length];
        for (int i = 0; i < plan.length; i++) expected[i] = plan[i] == KEEP ? null : plan[i];
        return expected;
    }
}

package mariculture.domain.fish;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import mariculture.domain.json.JsonObject;

/** Typed tank abilities and breeding needs read from species and mutation JSON. */
class FishAbilityTest {
    private static FishAbility ability(String json) {
        return FishAbility.parse(JsonObject.parse(json, "test"));
    }

    private static BreedingCondition need(String json) {
        return BreedingCondition.parse(JsonObject.parse(json, "test"));
    }

    private static FishEffects.Living player() {
        return new FishEffects.Living(true, false, 20, 2, false);
    }

    private static FishEffects.Living zombie() {
        return new FishEffects.Living(false, false, 20, 2, true);
    }

    @Test
    void potionDefaultsAndTargets() {
        FishAbility.Potion p = (FishAbility.Potion) ability("{\"type\":\"potion\",\"effect\":\"resistance\"}");
        assertEquals("minecraft:resistance", p.effect());
        assertEquals(new FishAbility.Area(4, FishAbility.Targets.PLAYERS, 100, FishAbility.When.ANY), p.area());
        assertEquals(List.of(new FishEffects.Potion("minecraft:resistance", 300, 0, true)), p.actions(player()));
        assertTrue(p.actions(zombie()).isEmpty());
    }

    @Test
    void damageHitsHostilesByDefault() {
        FishAbility.Damage d = (FishAbility.Damage) ability("{\"type\":\"damage\",\"amount\":2}");
        assertTrue(d.actions(player()).isEmpty());
        assertEquals(List.of(new FishEffects.Damage("generic", 2f)), d.actions(zombie()));
    }

    @Test
    void xpOnlyReachesPlayers() {
        FishAbility.Xp x = (FishAbility.Xp) ability("{\"type\":\"xp\",\"targets\":\"all\"}");
        assertEquals(1, x.actions(player()).size());
        assertTrue(x.actions(zombie()).isEmpty());
    }

    @Test
    void whenFollowsDayAndNight() {
        assertTrue(FishAbility.When.NIGHT.allows(false));
        assertFalse(FishAbility.When.NIGHT.allows(true));
        assertTrue(FishAbility.When.ANY.allows(true));
    }

    @Test
    void badAbilitiesNameTheProblem() {
        assertThrows(IllegalArgumentException.class, () -> ability("{\"type\":\"teleport\"}"));
        assertThrows(IllegalArgumentException.class, () -> ability("{\"type\":\"longevity\",\"percent\":150}"));
        assertThrows(IllegalArgumentException.class, () -> ability("{\"type\":\"heal\",\"amount\":0}"));
        assertThrows(IllegalArgumentException.class, () -> ability("{\"type\":\"potion\",\"effect\":\"speed\",\"radius\":40}"));
    }

    private record Ctx(int light, int height, int water, Salinity salinity, int temperature, boolean raining, String food)
            implements BreedingCondition.Context {
        @Override public boolean hasFood(String itemOrTag) { return itemOrTag.equals(food); }
    }

    @Test
    void breedingNeedsTestTheTank() {
        Ctx dark = new Ctx(2, -40, 32, Salinity.SALINE, 10, false, "minecraft:feather");
        assertTrue(need("{\"type\":\"light\",\"max\":4}").test(dark));
        assertFalse(need("{\"type\":\"light\",\"min\":12}").test(dark));
        assertTrue(need("{\"type\":\"height\",\"max\":-32}").test(dark));
        assertFalse(need("{\"type\":\"water\",\"min\":33}").test(dark));
        assertTrue(need("{\"type\":\"salinity\",\"is\":[\"saline\",\"brackish\"]}").test(dark));
        assertFalse(need("{\"type\":\"rain\"}").test(dark));
        assertTrue(need("{\"type\":\"food\",\"item\":\"feather\"}").test(dark));
        assertEquals(new BreedingCondition.Food("#c:gems"), need("{\"type\":\"food\",\"tag\":\"c:gems\"}"));
        assertNull(need("{\"type\":\"egg_tag\",\"tag\":\"x\"}"), "mutation-only types are left to Mutation");
    }

    @Test
    void mutationBreedingNeedsTheLayTag() {
        Mutation m = Mutation.parse("mariculture:test",
                "{\"schema\":1,\"parents\":[\"cod\",\"salmon\"],\"child\":\"tuna\",\"chance_percent\":10,"
                        + "\"conditions\":[{\"type\":\"light\",\"max\":4}]}", "test");
        assertEquals(List.of(new BreedingCondition.Light(0, 4)), m.breeding());
        Egg egg = new Egg(new String[] {"a", "a", "a", "a"}, new int[Genes.ALL.size()][4], 1, 0, 0, Map.of());
        assertFalse(m.conditionsMet(egg, Set.of()));
        assertTrue(m.conditionsMet(egg.withTag(m.layTag()), Set.of()));
    }

    @Test
    void everyMetallurgyFishHasAnAbilityAndBreedingNeeds() {
        int metal = 0;
        for (Species s : FishData.species()) {
            if (s.products().isEmpty() || !s.products().get(0).item().equals(mariculture.domain.fishery.Droplets.METAL)) continue;
            metal++;
            assertFalse(s.abilities().isEmpty(), s.id());
            assertFalse(s.breeding().isEmpty(), s.id());
        }
        assertEquals(25, metal);
    }
}

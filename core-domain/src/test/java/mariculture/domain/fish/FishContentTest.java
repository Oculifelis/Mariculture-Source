package mariculture.domain.fish;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;

/** The shipped data files parse and cross-reference (traced counts from 1.3.0). */
class FishContentTest {
    @Test
    void allSpeciesAndMutationsParse() {
        assertEquals(111, FishData.species().size());
        assertEquals(92, FishData.mutations().size());
    }

    @Test
    void optionalMetalSpeciesNeedTheirIngotTag() {
        FishRegistry without = FishData.registry();
        assertNull(without.species("mariculture:tinfish"));
        assertNotNull(without.species("mariculture:copperfish"));
        // A5: the Alex's Caves fish wait for Alex's Caves.
        assertNull(without.species("mariculture:acid_lamprey"));
        assertTrue(without.problems().stream().anyMatch(p -> p.contains("tinfish")));
        // Their mutations are disabled with them rather than pointing at a missing species.
        assertTrue(without.mutations().stream().noneMatch(m -> m.child().equals("mariculture:tinfish")));

        FishRegistry with = FishData.registry(Set.of("c:ingots/tin", "c:ingots/silver", "c:ingots/lead", "c:ingots/platinum",
                "c:ingots/nickel", "c:ingots/cobalt", "c:ingots/ardite", "c:ingots/osmium", "c:ingots/zinc", "botania:petals",
                "mariculture:present/alexscaves", "mariculture:present/thaumaturge", "c:ingots/manganese", "c:ingots/lutetium", "c:ingots/prometheum",
                "c:ingots/deep_iron", "c:ingots/infuscolium", "c:ingots/oureclase", "c:ingots/astral_silver", "c:ingots/carmot",
                "c:ingots/mithril", "c:ingots/rubracium", "c:ingots/orichalcum", "c:ingots/adamantine", "c:ingots/atlarus",
                "c:ingots/ignatius", "c:ingots/shadow_iron", "c:ingots/lemurite", "c:ingots/midasium", "c:ingots/vyroxeres",
                "c:ingots/ceruclase", "c:ingots/alduorite", "c:ingots/kalendrite", "c:ingots/vulcanite", "c:ingots/sanguinite",
                "c:ingots/eximite", "c:ingots/meutoite"));
        assertEquals(111, with.allSpecies().size());
        assertEquals(92, with.mutations().size());
        assertTrue(with.problems().isEmpty(), with.problems().toString());
    }

    @Test
    void metallurgyFishDropTheirOwnMoltenMetal() {
        Species fish = FishData.registry(Set.of("c:ingots/mithril")).species("mariculture:mithrilfish");
        Species.Product drop = fish.products().get(0);
        assertEquals(mariculture.domain.fishery.Droplets.METAL, drop.item());
        mariculture.domain.machine.Stack stack = mariculture.domain.machine.Stack.of(drop.item(), drop.count(), drop.data());
        mariculture.domain.fishery.Droplets.Droplet d = mariculture.domain.fishery.Droplets.of(stack);
        assertEquals("metallurgy:mithril", d.fluid());
        assertEquals(10, d.amount());
        assertTrue(mariculture.domain.fishery.Droplets.color(stack) != 0xFFFFFF);
        // A metal droplet with no fluid data melts into nothing.
        assertNull(mariculture.domain.fishery.Droplets.of(mariculture.domain.machine.Stack.of(drop.item(), 1, java.util.Map.of())));
        // Each metal fish has its own needs: Nether metals live in lava, End metals in liquid ender.
        FishRegistry all = FishData.registry(Set.of("c:ingots/vulcanite", "c:ingots/meutoite"));
        Species vulcanite = all.species("mariculture:vulcanitefish");
        Species meutoite = all.species("mariculture:meutoitefish");
        assertTrue(vulcanite.lava());
        assertEquals(java.util.List.of("minecraft:lava"), vulcanite.validFluids());
        assertEquals(java.util.List.of("mariculture:ender"), meutoite.validFluids());
        assertTrue(vulcanite.fertility() != fish.fertility() && meutoite.fertility() != fish.fertility());
    }

    @Test
    void codMatchesLegacyClass() {
        Species cod = FishData.registry().species("mariculture:cod");
        assertEquals(0, cod.legacyId());
        assertEquals(11, cod.temperatureBase());
        assertEquals(Salinity.SALINE, cod.salinityBase());
        assertEquals(20 * 1200, cod.defaultValue(Genes.LIFESPAN, null));
        assertEquals(5000, cod.fertility());
        assertEquals(25, cod.catchChance().percent(new CatchChance.Context(64, true, "overworld_like")));
    }

    @Test
    void conditionalCatchChanceFollowsCases() {
        Species night = FishData.registry().species("mariculture:nightfish");
        assertEquals(55, night.catchChance().percent(new CatchChance.Context(64, true, "end")));
        assertEquals(5, night.catchChance().percent(new CatchChance.Context(40, false, "overworld_like")));
        assertEquals(35, night.catchChance().percent(new CatchChance.Context(64, false, "overworld_like")));
        assertEquals(0, night.catchChance().percent(new CatchChance.Context(64, true, "overworld_like")));
    }

    @Test
    void malformedDataNamesTheField() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Species.parse("x:y", "{\"schema\":1,\"temperature\":{\"base\":\"hot\"},\"salinity\":{\"base\":\"fresh\",\"tolerance\":0},\"area_of_effect\":{},\"catch\":{},\"food\":{}}", "fish_species/y.json"));
        assertTrue(e.getMessage().contains("temperature"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Species.parse("x:y", "{\"schema\":2}", "f"));
        assertThrows(IllegalArgumentException.class, () -> Mutation.parse("x:m", "{\"schema\":1,\"parents\":[\"a\"],\"child\":\"b\",\"chance_percent\":5}", "f"));
    }
}

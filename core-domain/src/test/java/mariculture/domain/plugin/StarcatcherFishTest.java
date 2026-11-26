package mariculture.domain.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import mariculture.domain.fish.ShippedSpecies;
import mariculture.domain.fish.Species;

/** SC1 (PLUG-028): Mariculture species written as Starcatcher fish entries. */
class StarcatcherFishTest {
    private static Species load(String id) throws Exception {
        String json = Files.readString(Path.of("../content-source/data/mariculture/mariculture/fish_species/" + id + ".json"));
        return Species.parse(id, json, id + ".json");
    }

    private static String raw(String id) {
        return ShippedSpecies.byId(id).rawItem();
    }

    @Test
    void onlyCatchableSpeciesWithTheirOwnRawItemGetAnEntry() throws Exception {
        assertTrue(StarcatcherFish.eligible(load("bass"), raw("bass")));
        assertFalse(StarcatcherFish.eligible(load("aluminumfish"), raw("aluminumfish")), "never caught");
        assertFalse(StarcatcherFish.eligible(load("cod"), raw("cod")), "vanilla cod is Starcatcher's own");
    }

    @Test
    @SuppressWarnings("unchecked")
    void salineDayFishBitesInOceansByDay() throws Exception {
        Map<String, Object> j = StarcatcherFish.json(load("angelfish"), raw("angelfish"));
        assertEquals("uncommon", j.get("rarity"));
        assertEquals(Map.of("identifier", "mariculture:raw_angelfish"), ((Map<String, Object>) j.get("catch_info")).get("item"));
        List<Map<String, Object>> r = (List<Map<String, Object>>) j.get("restrictions");
        assertEquals("overworld", r.get(0).get("dimension_entry"));
        assertEquals(List.of("#minecraft:is_ocean"), r.get(1).get("biomes"));
        assertEquals("gui.guide.day", r.get(2).get("translation_override"));
        assertEquals(List.of("minecraft:water"), r.get(3).get("fluids"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void netherFishBitesInNetherLava() throws Exception {
        Map<String, Object> j = StarcatcherFish.json(load("netherfish"), raw("netherfish"));
        List<Map<String, Object>> r = (List<Map<String, Object>>) j.get("restrictions");
        assertEquals("the_nether", r.get(0).get("dimension_entry"));
        assertEquals(List.of("minecraft:lava"), r.get(r.size() - 1).get("fluids"));
        assertTrue(((Map<String, Object>) j.get("textures")).get("tank").toString().endsWith("tanks/nether.png"));
    }

    @Test
    void rodTierPicksRarity() {
        assertEquals("common", StarcatcherFish.rarity(Species.Rod.OLD));
        assertEquals("rare", StarcatcherFish.rarity(Species.Rod.DIRE));
        assertEquals("epic", StarcatcherFish.rarity(Species.Rod.FLUX));
    }

    @Test
    void compatTagsNameOnlyOtherModsOptionally() {
        for (CompatTags.Entry e : CompatTags.ITEM_TAGS) {
            for (String o : e.optional()) assertFalse(o.startsWith("mariculture:") || o.startsWith("minecraft:"), o);
        }
        assertTrue(CompatTags.ITEM_TAGS.stream().anyMatch(e -> e.tag().equals(CompatTags.KELP) && e.optional().contains("upgrade_aquatic:tongue_kelp")));
    }
}

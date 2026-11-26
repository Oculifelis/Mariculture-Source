package mariculture.domain.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import mariculture.domain.fish.ShippedSpecies;
import mariculture.domain.fish.Species;

/** T2 (PLUG-032): Mariculture species written as Tide fish entries. */
class TideFishTest {
    private static Species load(String id) throws Exception {
        String json = Files.readString(Path.of("../content-source/data/mariculture/mariculture/fish_species/" + id + ".json"));
        return Species.parse(id, json, id + ".json");
    }

    @Test
    @SuppressWarnings("unchecked")
    void salineDayFishIsSaltwaterByDay() throws Exception {
        Map<String, Object> j = TideFish.json("angelfish", load("angelfish"), ShippedSpecies.byId("angelfish").rawItem());
        assertEquals("mariculture:raw_angelfish", j.get("fish"));
        List<Map<String, Object>> c = (List<Map<String, Object>>) j.get("conditions");
        assertEquals(List.of("minecraft:overworld"), c.get(0).get("dimensions"));
        assertEquals("water", c.get(1).get("fluid"));
        assertEquals("tide:saltwater", c.get(2).get("type"));
        assertEquals("tide:time_of_day", c.get(3).get("type"));
        Map<String, Object> p = (Map<String, Object>) j.get("journal_profile");
        assertEquals("saltwater", p.get("group"));
        assertEquals("uncommon", p.get("rarity"));
        assertEquals("journal.description.mariculture.angelfish", p.get("description"));
        assertTrue(TideFish.description(load("angelfish")).contains("salt water"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void netherFishIsLava() throws Exception {
        Species sp = load("netherfish");
        Map<String, Object> j = TideFish.json("netherfish", sp, ShippedSpecies.byId("netherfish").rawItem());
        List<Map<String, Object>> c = (List<Map<String, Object>>) j.get("conditions");
        assertEquals(List.of("minecraft:the_nether"), c.get(0).get("dimensions"));
        assertEquals("lava", c.get(1).get("fluid"));
        assertEquals("lava", TideFish.group(sp));
    }
}

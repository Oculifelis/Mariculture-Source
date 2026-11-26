package mariculture.domain.content;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** The creative tab groups items like the legacy tabs instead of registration order. */
class CreativeOrderTest {
    @Test
    void groups() {
        assertEquals(CreativeOrder.Group.FISHING, CreativeOrder.group("titanium_fishing_rod"));
        assertEquals(CreativeOrder.Group.FISHING, CreativeOrder.group("bait_worm"));
        assertEquals(CreativeOrder.Group.FISH, CreativeOrder.group("raw_bass"));
        assertEquals(CreativeOrder.Group.FISH, CreativeOrder.group("fish_eggs"));
        assertEquals(CreativeOrder.Group.FOOD, CreativeOrder.group("sushi"));
        assertEquals(CreativeOrder.Group.MAGIC, CreativeOrder.group("mariculture:ring"));
        assertEquals(CreativeOrder.Group.DIVING, CreativeOrder.group("scuba_mask"));
        assertEquals(CreativeOrder.Group.MACHINES, CreativeOrder.group("crucible"));
        assertEquals(CreativeOrder.Group.MACHINES, CreativeOrder.group("basic_speed_upgrade"));
        assertEquals(CreativeOrder.Group.MATERIALS, CreativeOrder.group("titanium_ingot"));
        assertEquals(CreativeOrder.Group.BUILDING, CreativeOrder.group("limestone"));
        assertEquals(CreativeOrder.Group.BUCKETS, CreativeOrder.group("molten_titanium_bucket"));
    }

    @Test
    void sortIsStableByGroup() {
        List<String> ids = new ArrayList<>(List.of("molten_iron_bucket", "limestone", "titanium_ingot", "sushi", "bait_worm", "raw_bass"));
        CreativeOrder.sort(ids, s -> s);
        assertEquals(List.of("bait_worm", "raw_bass", "sushi", "titanium_ingot", "limestone", "molten_iron_bucket"), ids);
    }
}

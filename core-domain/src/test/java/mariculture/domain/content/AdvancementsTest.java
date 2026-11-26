package mariculture.domain.content;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Every advancement points at items and a parent that exist. */
class AdvancementsTest {
    @Test
    void resolves() {
        Set<String> ids = new HashSet<>();
        Catalog.blocks().forEach(b -> ids.add(b.id()));
        Catalog.items().forEach(i -> ids.add(i.id()));
        Set<String> tags = Catalog.itemTags().keySet();
        Set<String> seen = new HashSet<>();
        for (Advancements.Entry a : Advancements.all()) {
            assertTrue(a.parent() == null || seen.contains(a.parent()), a.id() + " parent");
            assertTrue(ids.contains(a.icon()), a.id() + " icon " + a.icon());
            for (String it : a.items()) {
                if (it.startsWith("#")) assertTrue(tags.contains(it.substring(1)), a.id() + " tag " + it);
                else assertTrue(ids.contains(it), a.id() + " item " + it);
            }
            assertTrue(seen.add(a.id()), "duplicate " + a.id());
        }
        assertEquals(1, Advancements.all().stream().filter(a -> a.parent() == null).count());
    }
}

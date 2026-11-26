package mariculture.domain.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import mariculture.domain.content.Catalog;
import mariculture.domain.machine.Stack;
import mariculture.domain.random.ScriptedRng;
import org.junit.jupiter.api.Test;

class RockhopperTest {
    @Test
    void dropsRawFishAndNeopreneOnAGoodPlayerKill() {
        // j = 2 + 1 (looting 1): three cod, and a neoprene for the player.
        List<Stack> drops = Rockhopper.drops(true, 1, new ScriptedRng().ints(2, 1, 0));
        assertEquals(List.of(Stack.of("minecraft:cod", 3), Stack.of(Rockhopper.NEOPRENE)), drops);
        // j = 2 on a player kill: a second batch of fish.
        assertEquals(List.of(Stack.of("minecraft:salmon", 2), Stack.of("minecraft:pufferfish", 2)),
                Rockhopper.drops(true, 0, new ScriptedRng().ints(2, 0, 1, 3)));
        // j = 0: nothing; not a player kill: no extra.
        assertTrue(Rockhopper.drops(true, 0, new ScriptedRng().ints(0, 0)).isEmpty());
        assertEquals(1, Rockhopper.drops(false, 0, new ScriptedRng().ints(2, 0, 0)).size());
    }

    @Test
    void halvesDamageFromMobsButNotPlayersOrArrows() {
        assertEquals(5.5f, Rockhopper.incomingDamage(10, true, false));
        assertEquals(10f, Rockhopper.incomingDamage(10, true, true));
        assertEquals(10f, Rockhopper.incomingDamage(10, false, false));
    }

    @Test
    void spawnEggAndFoodTagAreInTheCatalog() {
        assertTrue(Catalog.items().stream().anyMatch(i -> i.id().equals("rockhopper_spawn_egg")));
        assertTrue(Catalog.itemTags().get("mariculture:rockhopper_food").contains("#minecraft:fishes"));
        assertEquals(15, Rockhopper.MAX_HEALTH);
    }
}

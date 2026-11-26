package mariculture.domain.content;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PedestalTest {
    @Test
    void topAndBottomClicksFaceThatWay() {
        assertEquals(Face.UP, Pedestal.facing(Face.UP, 0f));
        assertEquals(Face.DOWN, Pedestal.facing(Face.DOWN, 1f));
    }

    @Test
    void sideClickEdgesStandItUpOrDown() {
        assertEquals(Face.UP, Pedestal.facing(Face.NORTH, 0.85f));
        assertEquals(Face.NORTH, Pedestal.facing(Face.NORTH, 0.849f));
        assertEquals(Face.DOWN, Pedestal.facing(Face.EAST, 0.15f));
        assertEquals(Face.EAST, Pedestal.facing(Face.EAST, 0.151f));
        assertEquals(Face.WEST, Pedestal.facing(Face.WEST, 0.5f));
    }
}

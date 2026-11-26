package mariculture.domain.factory;

import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.machine.Stack;

/**
 * The sawmill's blocks (factory/blocks/BlockCustom*.java, 1.3.0) and the paintbrush that repaints them
 * (factory/items/ItemPaintbrush.java). Each block keeps its {@link CustomFaces}; broken, it drops itself with them
 * (nothing in creative), and the pick-block copy carries them too. They mine with an axe, no tier, and mobs never spawn
 * on them. The light block glows at full brightness; the two energy blocks hold 25000 FE and pass what they hold on
 * to a random other neighbour each time energy comes in (TileCustomPowered.receiveEnergy).
 */
public final class CustomBlocks {
    public static final int RF_CAPACITY = 25000;
    public static final String PAINTBRUSH = "mariculture:paintbrush";
    public static final int PAINTBRUSH_USES = 128;
    public static final String BRUSH_BLOCK = "block", BRUSH_SIDE = "side";

    private CustomBlocks() {}

    public static boolean isCustom(String item) {
        for (Plans.Type t : Plans.Type.values()) if (t.blockId().equals(item)) return true;
        return false;
    }

    public static boolean isEnergy(String block) {
        return block.equals(Plans.Type.RF.blockId()) || block.equals(Plans.Type.RF_WALL.blockId());
    }

    /** Sneak-using the brush on a block loads it with that face: a sawmill block's own face, any other block as is. */
    public static Stack load(Stack brush, String block, Face side) {
        return brush.withData(Map.of(BRUSH_BLOCK, block, BRUSH_SIDE, side.ordinal()));
    }

    /** Sneak-using it in the air loads a see-through face. */
    public static Stack loadAir(Stack brush) {
        return load(brush, CustomFaces.AIR, Face.DOWN);
    }

    /**
     * Using a loaded brush on a face of a sawmill block: the new faces, or null when the brush is empty or the face
     * already looks like that (no use is spent then).
     */
    public static CustomFaces paint(CustomFaces faces, Face face, Stack brush) {
        String block = brush.stringData(BRUSH_BLOCK, null);
        if (block == null) return null;
        int s = brush.intData(BRUSH_SIDE, 0);
        Face side = s >= 0 && s < 6 ? Face.values()[s] : Face.DOWN;
        if (faces.block(face).equals(block) && faces.side(face) == side) return null;
        return faces.with(face, block, side);
    }
}

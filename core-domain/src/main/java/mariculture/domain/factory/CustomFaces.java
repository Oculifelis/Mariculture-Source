package mariculture.domain.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.machine.Stack;

/**
 * What each face of a sawmill-made block looks like (factory/tile/TileCustom.java, 1.3.0): for every face, in
 * {@link Face} order, the block whose texture it shows and which face of that block. Air (a feather in the sawmill)
 * leaves the face see-through. Hardness and blast resistance are the average of the six blocks (updateHardness).
 *
 * <p>Changed: legacy also kept a block meta for each face; modern blocks carry it in their id.
 */
public record CustomFaces(List<String> blocks, List<Integer> sides) {
    public static final String AIR = "minecraft:air";
    public static final String STONE = "minecraft:stone";
    public static final String BLOCKS = "faces", SIDES = "sides";

    public CustomFaces {
        if (blocks.size() != 6 || sides.size() != 6) throw new IllegalArgumentException("six faces");
        blocks = List.copyOf(blocks);
        sides = List.copyOf(sides);
    }

    public static CustomFaces of(String... six) {
        return new CustomFaces(List.of(six), List.of(0, 0, 0, 0, 0, 0));
    }

    /** All stone, what an unset legacy tile showed. */
    public static final CustomFaces DEFAULT = of(STONE, STONE, STONE, STONE, STONE, STONE);

    public String block(Face f) {
        return blocks.get(f.ordinal());
    }

    /** The face of the source block shown (0 down ... 5 east); the sawmill always picks 0, the bottom. */
    public Face side(Face f) {
        int s = sides.get(f.ordinal());
        return s >= 0 && s < 6 ? Face.values()[s] : Face.DOWN;
    }

    public CustomFaces with(Face f, String block, Face side) {
        List<String> b = new ArrayList<>(blocks);
        List<Integer> s = new ArrayList<>(sides);
        b.set(f.ordinal(), block);
        s.set(f.ordinal(), side.ordinal());
        return new CustomFaces(b, s);
    }

    /** BlockCustomHelper.rotateBlock about a horizontal axis: the sides turn (0,1,4,5,3,2). */
    public CustomFaces rotateHorizontal() {
        return permute(0, 1, 4, 5, 3, 2);
    }

    /** About the vertical axis given as up/down: top and bottom swap. */
    public CustomFaces flip() {
        return permute(1, 0, 2, 3, 4, 5);
    }

    private CustomFaces permute(int... order) {
        List<String> b = new ArrayList<>();
        List<Integer> s = new ArrayList<>();
        for (int i : order) {
            b.add(blocks.get(i));
            s.add(sides.get(i));
        }
        return new CustomFaces(b, s);
    }

    /** The average of six values (hardness or resistance; unbreakable sources count as 0). */
    public static float average(float[] six) {
        float t = 0;
        for (float f : six) t += Math.max(0, f);
        return t / 6;
    }

    public Map<String, Object> toData() {
        Map<String, Object> m = new HashMap<>();
        m.put(BLOCKS, List.copyOf(blocks));
        m.put(SIDES, List.copyOf(sides));
        return m;
    }

    /** From item or block entity data; missing or bad entries become stone (TileCustom.theBlocks). */
    public static CustomFaces fromData(Map<String, ?> data) {
        Object b = data.get(BLOCKS), s = data.get(SIDES);
        if (!(b instanceof List<?> bl) || bl.size() != 6) return DEFAULT;
        List<String> blocks = new ArrayList<>();
        List<Integer> sides = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            blocks.add(bl.get(i) instanceof String str && !str.isEmpty() ? str : STONE);
            sides.add(s instanceof List<?> sl && sl.size() == 6 && sl.get(i) instanceof Number n ? n.intValue() : 0);
        }
        return new CustomFaces(blocks, sides);
    }

    public static CustomFaces of(Stack stack) {
        return fromData(stack.data());
    }

    public Stack onto(String item, int count) {
        return Stack.of(item, count, toData());
    }
}

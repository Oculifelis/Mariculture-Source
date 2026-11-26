package mariculture.domain.factory;

import mariculture.domain.machine.Stack;

/**
 * Sawmill plans (core/lib/PlansMeta.java, factory/items/ItemPlan.java, 1.3.0): which shape the sawmill cuts, and how
 * many of it come out of one set of six blocks.
 *
 * <p>Changed: legacy kept every plan as one item told apart by data; here each plan is its own item
 * ({@code plan_<key>}, the legacy icon names).
 */
public final class Plans {
    public static final String PREFIX = "mariculture:plan_";
    public static final String BLANK = "mariculture:blank_plan";

    /** PlansMeta order; {@code block} is the custom block the sawmill makes, {@code size} ItemPlan.getStackSize. */
    public enum Type {
        FLOOR("floor", "custom_flooring", 9),
        BLOCK("block", "custom_block", 6),
        STAIRS("stairs", "custom_stairs", 4),
        SLABS("slabs", "custom_slab", 12),
        FENCE("fence", "custom_fence", 6),
        GATE("gate", "custom_gate", 2),
        WALL("wall", "custom_wall", 6),
        LIGHT("light", "custom_light", 6),
        RF("redstone", "custom_rf_block", 1),
        RF_WALL("redstonewall", "custom_rf_wall", 1);

        public final String key, block;
        public final int size;

        Type(String key, String block, int size) {
            this.key = key;
            this.block = block;
            this.size = size;
        }

        public String blockId() {
            return "mariculture:" + block;
        }

        /** The plan item's path, e.g. {@code plan_stairs}. */
        public String itemPath() {
            return "plan_" + key;
        }
    }

    private Plans() {}

    public static Stack of(Type type) {
        return Stack.of("mariculture:" + type.itemPath());
    }

    /** The plan's shape, or null when the stack is not a plan. */
    public static Type type(Stack plan) {
        for (Type t : Type.values()) if (plan.is("mariculture:" + t.itemPath())) return t;
        return null;
    }

    public static boolean isPlan(Stack s) {
        return type(s) != null;
    }

    /** What kind of block chalk is drawn on (PlansMeta.getPlanType). */
    public enum Kind { FENCE, GATE, SLAB, STAIRS, WALL, CARPET, OTHER }

    /**
     * The plan chalk copies from a block, or null. Walls on top of an energy receiver give the energy wall; other
     * blocks that are themselves energy receivers give the energy block, then light sources the light; any other
     * block without a block entity a plain block (PlansMeta.java:72-88).
     */
    public static Type forBlock(Kind kind, boolean energyBelow, boolean energyHere, boolean lit, boolean hasBlockEntity, boolean air) {
        return switch (kind) {
            case FENCE -> Type.FENCE;
            case GATE -> Type.GATE;
            case SLAB -> Type.SLABS;
            case STAIRS -> Type.STAIRS;
            case WALL -> energyBelow ? Type.RF_WALL : Type.WALL;
            case CARPET -> Type.FLOOR;
            case OTHER -> energyHere ? Type.RF : lit ? Type.LIGHT : !hasBlockEntity && !air ? Type.BLOCK : null;
        };
    }

    /** Chalk (ItemChalk: 64 uses). */
    public static final String CHALK = "mariculture:chalk";
    public static final int CHALK_USES = 64;
}

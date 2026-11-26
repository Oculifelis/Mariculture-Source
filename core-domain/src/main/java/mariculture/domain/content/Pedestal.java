package mariculture.domain.content;

/** Limestone pedestal placement (legacy BlockLimestone#onBlockPlaced, metas 10-15). */
public final class Pedestal {
    /** Clicks within this fraction of the top or bottom of a side face stand the pedestal up or down. */
    public static final float EDGE = 0.15f;

    private Pedestal() {}

    /**
     * The face the pedestal's bordered top points at.
     *
     * @param clickedFace face of the existing block that was clicked
     * @param hitY        height of the click within that face, 0 (bottom) to 1 (top)
     */
    public static Face facing(Face clickedFace, float hitY) {
        if (clickedFace.isVertical()) return clickedFace;
        if (hitY >= 1f - EDGE) return Face.UP;
        if (hitY <= EDGE) return Face.DOWN;
        return clickedFace;
    }
}

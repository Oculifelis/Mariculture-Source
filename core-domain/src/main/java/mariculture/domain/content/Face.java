package mariculture.domain.content;

/** Block faces in legacy side-index order (0 down, 1 up, 2 north, 3 south, 4 west, 5 east). */
public enum Face {
    DOWN, UP, NORTH, SOUTH, WEST, EAST;

    public boolean isVertical() {
        return this == DOWN || this == UP;
    }
}

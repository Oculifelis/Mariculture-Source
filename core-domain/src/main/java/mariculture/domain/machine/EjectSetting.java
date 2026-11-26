package mariculture.domain.machine;

/** core/gui/feature/FeatureEject.EjectSetting: what a machine pushes into neighbouring blocks. */
public enum EjectSetting {
    NONE, ITEM, FLUID, ITEM_AND_FLUID;

    public boolean ejectsItems() {
        return this == ITEM || this == ITEM_AND_FLUID;
    }

    public boolean ejectsFluids() {
        return this == FLUID || this == ITEM_AND_FLUID;
    }

    /** Legacy toggle: a machine that only ejects one kind flips between NONE and that kind; ITEM_AND_FLUID cycles all four. */
    public EjectSetting toggle(EjectSetting supported) {
        return switch (supported) {
            case ITEM -> this == ITEM ? NONE : ITEM;
            case FLUID -> this == FLUID ? NONE : FLUID;
            case ITEM_AND_FLUID -> values()[(ordinal() + 1) % values().length];
            case NONE -> NONE;
        };
    }

    public static EjectSetting byOrdinal(int i) {
        return i >= 0 && i < values().length ? values()[i] : NONE;
    }
}

package mariculture.domain.machine;

/** A fluid id and an amount in millibuckets. */
public record FluidAmount(String fluid, int amount) {
    public static final FluidAmount NONE = new FluidAmount(null, 0);

    public boolean isEmpty() {
        return fluid == null || amount <= 0;
    }
}

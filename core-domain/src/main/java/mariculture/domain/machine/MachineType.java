package mariculture.domain.machine;

import java.util.Objects;
import java.util.function.Supplier;

import mariculture.domain.content.Ids;

/**
 * A machine block kind: its logic factory and its screen. The block itself (strength, textures, model)
 * is a {@link mariculture.domain.content.BlockSpec} with the same id and shape MACHINE.
 *
 * @param layout null for machines without a screen
 */
public record MachineType(String id, Supplier<Machine> factory, Layout layout) {
    public MachineType {
        Ids.requireValid(id);
        Objects.requireNonNull(factory);
    }

    public Machine create() {
        return factory.get();
    }

    public boolean hasScreen() {
        return layout != null;
    }
}

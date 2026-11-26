package mariculture.domain.metal;

import mariculture.domain.machine.SlotInfo;

import mariculture.domain.machine.Layout;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;

/** Storage bookshelf (core/tile/TileBookshelf): nine slots of storage behind a screen. */
public final class Bookshelf extends Machine {
    public static final MachineType TYPE = new MachineType("storage_bookshelf", Bookshelf::new, Layout.builder(150).grid(0, 9, 1, 8, 30).build());

    @Override
    public MachineType type() {
        return TYPE;
    }

    @Override
    public int slotCount() {
        return 9;
    }

    @Override
    public SlotInfo slotInfo(int slot) {
        return SlotInfo.of(SlotInfo.Role.STORAGE, "book", java.util.List.of("minecraft:book", "minecraft:written_book"), null);
    }
}

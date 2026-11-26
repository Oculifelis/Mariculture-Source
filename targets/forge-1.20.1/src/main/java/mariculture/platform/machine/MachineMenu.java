package mariculture.platform.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import mariculture.domain.machine.Layout;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.Tank;
import mariculture.platform.ModContent;

/**
 * The one menu for every machine screen, laid out from the machine type's {@link Layout}. Gauges and tank
 * contents travel as pairs of 16-bit data slots (vanilla sends data slots as shorts).
 */
public class MachineMenu extends AbstractContainerMenu {
    private final MachineBlockEntity be;
    private final Layout layout;
    private final int machineSlots;
    private final int gaugeCount;
    private final int tankCount;
    private final int[] values;

    public MachineMenu(int id, Inventory inv, BlockPos pos) {
        this(id, inv, (MachineBlockEntity) inv.player.level().getBlockEntity(pos));
    }

    public MachineMenu(int id, Inventory inv, MachineBlockEntity be) {
        super(ModContent.machineMenu(), id);
        this.be = be;
        this.layout = be.type().layout();
        Machine machine = be.machine();
        this.gaugeCount = machine.gauges().length;
        this.tankCount = machine.tanks().size();
        this.values = new int[gaugeCount + tankCount * 3];
        int slots = 0;
        for (Layout.Widget w : layout.widgets()) {
            if (w instanceof Layout.SlotW s) {
                addSlot(new MachineSlot(be, machine, s.slot(), s.x(), s.y()));
                slots++;
            }
        }
        this.machineSlots = slots;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, layout.inventoryX() + c * 18, layout.inventoryY() + r * 18));
        }
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, layout.inventoryX() + c * 18, layout.inventoryY() + 58));
        addDataSlots(new ContainerData() {
            @Override
            public int get(int index) {
                int v = values[index / 2];
                return index % 2 == 0 ? v & 0xFFFF : (v >>> 16) & 0xFFFF;
            }

            @Override
            public void set(int index, int value) {
                int i = index / 2;
                values[i] = index % 2 == 0 ? (values[i] & 0xFFFF0000) | (value & 0xFFFF) : (values[i] & 0xFFFF) | (value << 16);
            }

            @Override
            public int getCount() {
                return values.length * 2;
            }
        });
    }

    @Override
    public void broadcastChanges() {
        if (!be.getLevel().isClientSide) {
            Machine machine = be.machine();
            int[] g = machine.gauges();
            System.arraycopy(g, 0, values, 0, Math.min(g.length, gaugeCount));
            for (int t = 0; t < tankCount; t++) {
                Tank tank = machine.tanks().get(t);
                var f = MachineBlockEntity.fluid(tank.fluid());
                values[gaugeCount + t * 3] = f == null || tank.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(f);
                values[gaugeCount + t * 3 + 1] = tank.amount();
                values[gaugeCount + t * 3 + 2] = tank.capacity();
            }
        }
        super.broadcastChanges();
    }

    public Layout layout() {
        return layout;
    }

    public int gauge(int i) {
        return i >= 0 && i < gaugeCount ? values[i] : 0;
    }

    /** Registry id of the tank's fluid, or -1 when empty. */
    public int tankFluid(int t) {
        return values[gaugeCount + t * 3];
    }

    public int tankAmount(int t) {
        return values[gaugeCount + t * 3 + 1];
    }

    public int tankCapacity(int t) {
        return values[gaugeCount + t * 3 + 2];
    }

    public MachineBlockEntity blockEntity() {
        return be;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        be.machine().button(id);
        be.setChanged();
        return true;
    }

    /** Ghost slots (legacy SlotFake): set a one-item copy, right-click clears, empty-hand click presses the slot's button. */
    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < machineSlots && slots.get(slotId) instanceof MachineSlot ms && be.machine().isGhost(ms.index)) {
            if (type != ClickType.PICKUP) return;
            ItemStack carried = getCarried();
            if (button == 1) {
                ms.set(ItemStack.EMPTY);
            } else if (carried.isEmpty()) {
                int id = be.machine().ghostButton(ms.index);
                if (id >= 0) be.machine().button(id);
            } else {
                ms.set(carried.copyWithCount(1));
            }
            be.setChanged();
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(be, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < machineSlots && be.machine().isGhost(((MachineSlot) slot).index)) return ItemStack.EMPTY;
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            // The machine names the slots made for this stack first (fuel to fuel, rod to rod); stacks top up
            // matching slots before filling empty ones.
            int[] order = be.machine().quickMoveOrder(mariculture.platform.Stacks.toDomain(stack));
            for (int pass = 0; pass < 2; pass++) {
                for (int machineSlot : order) {
                    if (stack.isEmpty()) break;
                    Slot target = menuSlot(machineSlot);
                    if (target == null || !target.mayPlace(stack)) continue;
                    int limit = Math.min(target.getMaxStackSize(stack), stack.getMaxStackSize());
                    ItemStack in = target.getItem();
                    if (pass == 1 && in.isEmpty()) {
                        target.set(stack.split(Math.min(limit, stack.getCount())));
                        moved = true;
                    } else if (pass == 0 && !in.isEmpty() && ItemStack.isSameItemSameTags(in, stack) && in.getCount() < limit) {
                        int n = Math.min(limit - in.getCount(), stack.getCount());
                        in.grow(n);
                        stack.shrink(n);
                        target.setChanged();
                        moved = true;
                    }
                }
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    private mariculture.domain.machine.SlotInfo[] infos;

    /** Menu slots 0 to this count - 1 are the machine's. */
    public int machineSlotCount() {
        return machineSlots;
    }

    /** What the machine slot at this menu index is for (border, faint items, tooltip). */
    public mariculture.domain.machine.SlotInfo slotInfo(int menuIndex) {
        if (infos == null) {
            infos = new mariculture.domain.machine.SlotInfo[machineSlots];
            for (int i = 0; i < machineSlots; i++) infos[i] = be.machine().slotInfo(((MachineSlot) slots.get(i)).index);
        }
        return infos[menuIndex];
    }

    /** The menu slot showing a machine slot, or null when the layout has none for it. */
    private Slot menuSlot(int machineSlot) {
        for (int i = 0; i < machineSlots; i++) if (((MachineSlot) slots.get(i)).index == machineSlot) return slots.get(i);
        return null;
    }

    /** A machine slot honouring the machine's validity and per-slot limit. */
    static final class MachineSlot extends Slot {
        private final Machine machine;
        final int index;

        MachineSlot(Container container, Machine machine, int index, int x, int y) {
            super(container, index, x, y);
            this.machine = machine;
            this.index = index;
        }

        @Override
        public boolean mayPickup(Player player) {
            return !machine.isGhost(index);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(index, stack);
        }

        @Override
        public int getMaxStackSize() {
            return machine.slotLimit(index);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.min(machine.slotLimit(index), stack.getMaxStackSize());
        }
    }
}

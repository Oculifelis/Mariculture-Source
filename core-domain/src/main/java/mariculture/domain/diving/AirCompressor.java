package mariculture.domain.diving;

import java.util.List;
import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.machine.Battery;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;
import mariculture.domain.maritech.EnergyItem;

/**
 * Air compressor (maritech/tile/TileAirCompressor.java, extensions/blocks/ExtensionRenderedMachineMulti.java:90-121,
 * 1.3.0): a 10000 energy buffer (FE, legacy RF) that every 20 ticks, with 1000 stored, uses 1000 and stores one air
 * up to 480. It takes energy only while it has room for air. A right-click with an energy item moves up to 5000 into
 * it; a right-click with a scuba tank moves air into the tank, all it can, or one with sneaking.
 *
 * <p>Changes: one block instead of the 2x2x2 multiblock of two bases and two tops (like the vat and crucible here),
 * made with one recipe; no screen, Jade shows the air and energy. Legacy receiveEnergy stored the energy but always
 * answered 0, so cables kept what they gave (free energy) and batteries could never charge it by hand (BUG-065); it
 * now answers what it took.
 */
public final class AirCompressor extends Machine {
    public static final MachineType TYPE = new MachineType("air_compressor", AirCompressor::new, null);
    public static final int ENERGY = 10000, USE = 1000, EVERY = 20, MAX_AIR = 480, CLICK_ENERGY = 5000;
    private static final int[] NONE = {};
    private final Battery battery = new Battery(ENERGY) {
        @Override
        public int receive(int max, boolean simulate) {
            return air < MAX_AIR ? super.receive(max, simulate) : 0;
        }
    };
    private int tick;
    private int air;

    @Override
    public MachineType type() {
        return TYPE;
    }

    @Override
    public int slotCount() {
        return 0;
    }

    @Override
    public int[] slots(Face side) {
        return NONE;
    }

    @Override
    public Battery battery() {
        return battery;
    }

    public int air() {
        return air;
    }

    @Override
    public void tick(MachineWorld world) {
        tick++;
        if (tick % EVERY == 0 && battery.use(USE)) {
            if (air < MAX_AIR) air++;
            slotChanged(-1);
        }
    }

    @Override
    public Stack useHeld(Stack held, boolean sneaking) {
        if (EnergyItem.spec(held) != null) {
            Stack after = EnergyItem.charge(battery, held, CLICK_ENERGY);
            if (after != null) slotChanged(-1);
            return after == null ? held : after;
        }
        if (held.is("mariculture:" + Scuba.TANK)) {
            int want = Scuba.TANK_AIR - Scuba.air(held);
            int moved = Math.min(air, sneaking ? Math.min(1, want) : want);
            if (moved <= 0) return held;
            air -= moved;
            slotChanged(-1);
            return Scuba.withAir(held, Scuba.air(held) + moved);
        }
        return null;
    }

    @Override
    public void save(Map<String, Object> out) {
        super.save(out);
        out.put("StoredAir", air);
    }

    @Override
    public void load(Map<String, ?> in) {
        super.load(in);
        air = Math.max(0, Math.min(MAX_AIR, integer(in.get("StoredAir"), 0)));
    }

    @Override
    public void describe(List<mariculture.domain.view.RecipeViews.Line> out) {
        out.add(new mariculture.domain.view.RecipeViews.Line("jade.mariculture.air", air, MAX_AIR));
        out.add(new mariculture.domain.view.RecipeViews.Line("jade.mariculture.energy", battery.stored(), battery.capacity()));
    }
}

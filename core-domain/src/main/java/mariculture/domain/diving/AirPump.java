package mariculture.domain.diving;

import mariculture.domain.content.Face;
import mariculture.domain.fluid.NaturalGas;
import mariculture.domain.machine.Battery;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Tank;

/**
 * Air pump (core/tile/TileAirPump.java, 1.3.0): runs on energy (FE, legacy RF) from a 2000 buffer.
 *
 * <p>Every 300 ticks with at least 100 stored it uses 100 and gives full air to every player within 64 blocks
 * across and 128 up or down who wears the diving helmet and the diving air supply and is in water (updateEntity:91,
 * supplyWithAir:34). Every 20 ticks with at least 100 stored it uses 100 and takes one natural gas block from 6
 * blocks round it and up to 9 above it into its 16000 mB tank as 1000 mB of natural gas, then offers gas to the
 * neighbours; every 100 ticks it offers gas anyway (updateEntity:102, suckUpGas:51, ejectFluids:128).
 *
 * <p>A right-click winds it by hand for 100 unless its wheel is still turning; the wheel turns for 63 ticks after a
 * winding and after each air or gas cycle (BlockRenderedMachine.onBlockActivated:180, updateAnimation:133).
 *
 * <p>Changed: legacy only sucked up gas with MariTech loaded (HIGH_TECH_ENABLED); here it always does, since the
 * gas block exists without MariTech. The wheel animation is not ported; the model is still.
 */
public final class AirPump extends Machine {
    public static final MachineType TYPE = new MachineType("air_pump", AirPump::new, null);
    public static final int ENERGY = 2000, TANK = 16000, USE = 100, AIR_EVERY = 300, GAS_EVERY = 20, EJECT_EVERY = 100, GAS_PER_BLOCK = 1000;
    public static final double RANGE_XZ = 64, RANGE_Y = 128;
    public static final int GAS_RADIUS = 6, GAS_HEIGHT = 10;
    /** BlockTransferHelper.ejectFluid amounts tried in order. */
    public static final int[] EJECT = {8000, 4000, 2000, 1000, 100, 20, 1};
    private static final int[] NONE = {};
    private final Battery battery = new Battery(ENERGY);
    private final Tank tank = new Tank(TANK, f -> mariculture.domain.fluid.Fluids.NATURAL_GAS.equals(f));
    /** The wheel turns 0.1 radians a tick up to a full turn (updateEntity:73). */
    public static final int WHEEL_TICKS = 63;
    private int tick;
    private int wheel;

    public AirPump() {
        tanks.add(tank);
    }

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

    @Override
    public boolean canFill(int tank, Face side, String fluid) {
        return side == null;
    }

    /** Winding by hand: 100 energy unless the wheel still turns; the click is used either way. */
    @Override
    public java.util.List<mariculture.domain.machine.Stack> onUse() {
        if (wheel == 0) battery.receive(USE, false);
        turn();
        return java.util.List.of();
    }

    private void turn() {
        if (wheel == 0) wheel = WHEEL_TICKS;
    }

    public boolean turning() {
        return wheel > 0;
    }

    @Override
    public void tick(MachineWorld world) {
        if (wheel > 0) wheel--;
        if (battery.stored() <= 0) return;
        tick++;
        if (tick % AIR_EVERY == 0 && battery.use(USE)) {
            world.refillAir(RANGE_XZ, RANGE_Y, RANGE_XZ, "mariculture:" + Diving.HELMET, "mariculture:" + Diving.TOP);
            turn();
        }
        if (tick % GAS_EVERY == 0) {
            if (battery.use(USE)) {
                if (suckUpGas(world)) eject(world);
                turn();
            }
            if (tick % EJECT_EVERY == 0) eject(world);
        }
    }

    /** suckUpGas: the first gas block in x, z -6..6 and y 0..9 (x outermost), if the tank takes 1000 mB. */
    public boolean suckUpGas(MachineWorld world) {
        if (tank.fill(mariculture.domain.fluid.Fluids.NATURAL_GAS, GAS_PER_BLOCK, true) < GAS_PER_BLOCK) return false;
        String gas = "mariculture:" + NaturalGas.BLOCK;
        for (int x = -GAS_RADIUS; x <= GAS_RADIUS; x++) {
            for (int z = -GAS_RADIUS; z <= GAS_RADIUS; z++) {
                for (int y = 0; y < GAS_HEIGHT; y++) {
                    if (gas.equals(world.block(x, y, z))) {
                        tank.fill(mariculture.domain.fluid.Fluids.NATURAL_GAS, GAS_PER_BLOCK, false);
                        world.setBlock(x, y, z, "minecraft:air");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void eject(MachineWorld world) {
        if (!tank.isEmpty()) world.ejectFluidExact(tank, EJECT, null);
    }

    @Override
    public void save(java.util.Map<String, Object> out) {
        super.save(out);
        out.put("Tick", tick);
    }

    @Override
    public void load(java.util.Map<String, ?> in) {
        super.load(in);
        tick = integer(in.get("Tick"), 0);
    }
}

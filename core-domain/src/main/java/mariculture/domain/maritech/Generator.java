package mariculture.domain.maritech;

import java.util.ArrayList;
import java.util.List;

import mariculture.domain.content.Face;
import mariculture.domain.factory.Geyser;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;

/**
 * Generator (maritech/tile/TileGenerator.java, 1.3.0): no buffer and no tick. Energy its rotors hand it goes straight
 * to the energy receivers (FE, legacy RF) on every side but the back, where the rotors are (addEnergy:277).
 *
 * <p>Fixed: legacy offered the whole amount to every side, so two machines beside it each got all of it (energy made
 * twice, BUG-067); here the sides share it, each taking from what the ones before left, in random side order.
 */
public final class Generator extends Machine {
    public static final MachineType TYPE = new MachineType("generator", Generator::new, null);
    private static final int[] NONE = {};

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

    /**
     * Gives {@code energy} from the generator at an offset from the caller to its neighbours; returns how much they
     * took.
     */
    public static int output(MachineWorld world, int gx, int gy, int gz, int energy) {
        Face back = Geyser.opposite(world.facingAt(gx, gy, gz));
        List<Face> sides = new ArrayList<>(List.of(Face.values()));
        sides.remove(back);
        for (int i = sides.size() - 1; i > 0; i--) java.util.Collections.swap(sides, i, world.random().nextInt(i + 1));
        int left = energy;
        for (Face f : sides) {
            if (left <= 0) break;
            int[] o = Geyser.offset(f);
            left -= Math.max(0, world.insertEnergy(gx + o[0], gy + o[1], gz + o[2], Geyser.opposite(f), left));
        }
        return energy - left;
    }
}

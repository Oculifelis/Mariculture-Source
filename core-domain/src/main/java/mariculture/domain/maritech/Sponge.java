package mariculture.domain.maritech;

import java.util.List;
import java.util.Map;

import mariculture.domain.machine.Battery;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.view.RecipeViews;

/**
 * Mechanized sponge (maritech/tile/TileSponge.java, 1.3.0): every 50 ticks it looks up to 47 blocks east, west, south,
 * north and up for vanilla sponges (updateCoords:24). With all five found, every 200 ticks it asks for
 * width x depth x height x 40 FE (tick:79) and, once it holds that much, removes every liquid and sponge in the box
 * from its own level up to the top sponge (clearWater:58). It takes at most 5000 FE a tick and holds nothing while a
 * marker is missing.
 *
 * <p>Changed: wet sponges count as markers too and are cleared with the dry ones; a modern sponge placed in water
 * soaks it up and turns wet at once, which 1.7 sponges did not.
 */
public final class Sponge extends Machine {
    public static final MachineType TYPE = new MachineType("mechanized_sponge", Sponge::new, null);
    public static final String MARKER = "minecraft:sponge", WET = "minecraft:wet_sponge";
    public static final int RANGE = 48, LOOK = 50, CLEAR = 200, PER_BLOCK = 40, RECEIVE = 5000;
    private final Battery battery = new Battery(0, RECEIVE, Integer.MAX_VALUE);
    /** Offsets of the markers: +x, -x, +z, -z, +y; -1 distance when missing. */
    private final int[] found = {-1, -1, -1, -1, -1};
    private int tick;

    @Override
    public MachineType type() {
        return TYPE;
    }

    @Override
    public int slotCount() {
        return 0;
    }

    @Override
    public Battery battery() {
        return battery;
    }

    private static final int[][] DIRS = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}};

    private void look(MachineWorld world) {
        for (int d = 0; d < DIRS.length; d++) {
            found[d] = -1;
            for (int i = 0; i < RANGE; i++) {
                if (isMarker(world.block(DIRS[d][0] * i, DIRS[d][1] * i, DIRS[d][2] * i))) {
                    found[d] = i;
                    break;
                }
            }
        }
    }

    public static boolean isMarker(String block) {
        return block.equals(MARKER) || block.equals(WET);
    }

    private boolean allFound() {
        for (int f : found) if (f < 0) return false;
        return true;
    }

    /** The energy a clear costs: the distances between the markers multiplied, x 40. */
    public int cost() {
        long c = (long) (found[0] + found[1]) * (found[2] + found[3]) * found[4] * PER_BLOCK;
        return (int) Math.min(Integer.MAX_VALUE, c);
    }

    @Override
    public void tick(MachineWorld world) {
        if (tick % LOOK == 0) look(world);
        tick++;
        if (tick % CLEAR != 0) return;
        if (!allFound()) {
            battery.setCapacity(0);
            return;
        }
        int cost = cost();
        battery.setCapacity(cost);
        if (battery.stored() >= cost) {
            clear(world);
            battery.use(cost);
            battery.setCapacity(0);
        }
    }

    private void clear(MachineWorld world) {
        for (int x = -found[1]; x <= found[0]; x++) {
            for (int z = -found[3]; z <= found[2]; z++) {
                for (int y = 0; y <= found[4]; y++) {
                    if (!world.fluid(x, y, z).isEmpty() || isMarker(world.block(x, y, z))) world.setBlock(x, y, z, "minecraft:air");
                }
            }
        }
    }

    @Override
    public void describe(List<RecipeViews.Line> out) {
        out.add(new RecipeViews.Line("jade.mariculture.energy", battery.stored(), battery.capacity()));
    }

    @Override
    public void save(Map<String, Object> out) {
        super.save(out);
        out.put("Capacity", battery.capacity());
    }

    @Override
    public void load(Map<String, ?> in) {
        battery.setCapacity(in.get("Capacity") instanceof Number n ? n.intValue() : 0);
        super.load(in);
    }
}

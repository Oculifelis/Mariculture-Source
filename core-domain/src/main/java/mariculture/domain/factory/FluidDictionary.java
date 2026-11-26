package mariculture.domain.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mariculture.domain.config.Settings;
import mariculture.domain.content.Face;
import mariculture.domain.machine.Layout;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;
import mariculture.domain.machine.Tank;

/**
 * The fluid dictionary (factory/tile/TileDictionaryFluid.java, core/handlers/FluidDicHandler.java, 1.3.0): a tank
 * set to one fluid that turns fluids of the same family into it, by each one's ratio (legacy: water 2000, xp juice
 * 200, liquid xp 1000, mob essence 666). Right-click steps the fluid it makes through the configured fluids that
 * exist. Families and ratios are config {@code factory.fluid_dictionary}, entries {@code family=fluid=ratio}.
 *
 * <p>Changed: legacy only worked while something pushed fluid into it, and gave the result straight back to that
 * neighbour. Here what comes in waits in a small buffer, and each tick a full ratio of it becomes the target's ratio,
 * pushed into any neighbour that takes all of it.
 */
public final class FluidDictionary extends Machine {
    public static final MachineType TYPE = new MachineType("fluid_dictionary", FluidDictionary::new, Layout.builder(176).build());

    public record Entry(String family, String fluid, int ratio) {}

    private final Tank target = new Tank(1);
    private final Tank buffer = new Tank(1, this::accepts);
    private List<Entry> available;

    public FluidDictionary() {
        tanks.add(target);
        tanks.add(buffer);
    }

    /** The configured entries; malformed ones are skipped. */
    public static List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        for (String s : Settings.FLUID_DICTIONARY.get()) {
            String[] p = s.split("=");
            if (p.length != 3) continue;
            try {
                int r = Integer.parseInt(p[2].trim());
                if (r > 0) out.add(new Entry(p[0].trim(), p[1].trim(), r));
            } catch (NumberFormatException ignored) {
                // skipped
            }
        }
        return out;
    }

    public static Entry entry(String fluid) {
        for (Entry e : entries()) if (e.fluid().equals(fluid)) return e;
        return null;
    }

    private boolean accepts(String fluid) {
        Entry in = entry(fluid), out = entry(target.fluid());
        return in != null && out != null && in.family().equals(out.family());
    }

    @Override
    public MachineType type() {
        return TYPE;
    }

    @Override
    public int slotCount() {
        return 0;
    }

    public String target() {
        return target.fluid();
    }

    public Tank buffer() {
        return buffer;
    }

    /** The buffer holds one ratio of the largest family member, so any of them fits whole. */
    private void sizeBuffer() {
        Entry out = entry(target.fluid());
        int max = 1;
        if (out != null) for (Entry e : entries()) if (e.family().equals(out.family())) max = Math.max(max, e.ratio());
        buffer.setCapacity(max);
    }

    @Override
    public boolean canFill(int tank, Face side, String fluid) {
        return tank == 1;
    }

    @Override
    public boolean canDrain(int tank, Face side) {
        return false;
    }

    @Override
    public void tick(MachineWorld world) {
        if (available == null) {
            available = new ArrayList<>();
            for (Entry e : entries()) if (world.fluidExists(e.fluid())) available.add(e);
            if (target.isEmpty() && !available.isEmpty()) target.set(available.get(0).fluid(), 1);
            sizeBuffer();
        }
        Entry in = entry(buffer.fluid()), out = entry(target.fluid());
        if (in == null || out == null || buffer.amount() < in.ratio()) return;
        for (Face f : world.shuffledFaces()) {
            if (world.fillTo(f, out.fluid(), out.ratio(), false) == out.ratio()) {
                world.fillTo(f, out.fluid(), out.ratio(), true);
                buffer.drain(in.ratio(), false);
                return;
            }
        }
    }

    /** BlockTank.onBlockActivated: the next configured fluid that exists, wrapping round. */
    @Override
    public List<Stack> onUse() {
        if (available == null || available.isEmpty()) return List.of();
        int at = -1;
        for (int i = 0; i < available.size(); i++) if (available.get(i).fluid().equals(target.fluid())) at = i;
        target.set(available.get((at + 1) % available.size()).fluid(), 1);
        sizeBuffer();
        if (!buffer.isEmpty() && !accepts(buffer.fluid())) buffer.set(buffer.fluid(), 0);
        return List.of();
    }

    @Override
    public boolean usesClick() {
        return true;
    }

    @Override
    public void load(Map<String, ?> in) {
        buffer.setCapacity(Integer.MAX_VALUE);
        super.load(in);
        sizeBuffer();
    }
}

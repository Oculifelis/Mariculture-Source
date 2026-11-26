package mariculture.domain.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import mariculture.domain.content.Face;
import mariculture.domain.fish.FishEffects;
import mariculture.domain.fish.Salinity;
import mariculture.domain.random.Rng;

/** A world of still water (or air) around a machine, for domain machine tests. Records drops. */
public class FakeWorld implements MachineWorld {
    public long time;
    public boolean day = true;
    public boolean powered;
    public String fluidEverywhere = "minecraft:water";
    public int temperature = 10;
    public Salinity salinity = Salinity.SALINE;
    public final List<Stack> drops = new ArrayList<>();
    public final List<FishEffects.WorldAction> worldActions = new ArrayList<>();
    private final Rng rng;

    public FakeWorld(long seed) {
        this.rng = Rng.of(new Random(seed));
    }

    @Override public Rng random() { return rng; }
    @Override public long gameTime() { return time; }
    @Override public boolean isDay() { return day; }
    @Override public boolean isRaining() { return false; }
    @Override public boolean poweredByRedstone() { return powered; }
    @Override public String dimension() { return "minecraft:overworld"; }
    @Override public boolean isNether() { return false; }
    @Override public boolean isEnd() { return false; }
    @Override public int y() { return 62; }
    @Override public int biomeTemperature() { return temperature; }
    @Override public Salinity salinity() { return salinity; }
    @Override public int temperature() { return temperature; }
    @Override public String block(int dx, int dy, int dz) { return fluidEverywhere.isEmpty() ? "minecraft:air" : fluidEverywhere; }
    @Override public boolean blockHasTag(int dx, int dy, int dz, String tag) { return false; }
    @Override public String fluid(int dx, int dy, int dz) { return (dx | dy | dz) == 0 ? "" : fluidEverywhere; }
    @Override public boolean isFluidSource(int dx, int dy, int dz) { return true; }
    @Override public boolean isAir(int dx, int dy, int dz) { return false; }
    @Override public void setBlock(int dx, int dy, int dz, String block) {}
    /** Neighbouring machines by "dx,dy,dz". */
    public final java.util.Map<String, Machine> machines = new java.util.HashMap<>();
    @Override public Machine machine(int dx, int dy, int dz) { return machines.get(dx + "," + dy + "," + dz); }
    /** The fluid handler below (crucible fuel, autohammer experience). */
    public FluidAmount below;
    @Override public FluidAmount drainFrom(Face face, int max, boolean execute) {
        if (face != Face.DOWN || below == null || below.amount() <= 0) return null;
        int n = Math.min(max, below.amount());
        FluidAmount out = new FluidAmount(below.fluid(), n);
        if (execute) below = new FluidAmount(below.fluid(), below.amount() - n);
        return out;
    }
    @Override public boolean itemHasTag(String item, String tag) { return false; }
    @Override public void drop(Stack stack) { drops.add(stack); }
    /** Last box passed to guardSpawns (null when cleared); guardCalls counts the calls. */
    public int[] guard;
    public int guardCalls;
    @Override public void guardSpawns(int[] box) { guard = box; guardCalls++; }
    @Override public Stack ejectToNeighbours(Stack stack) { return stack; }
    /** Items ejected through a face (fish sorter); every face accepts everything. */
    public final java.util.Map<Face, List<Stack>> ejected = new java.util.EnumMap<>(Face.class);
    @Override public Stack ejectTo(Face face, Stack stack) { ejected.computeIfAbsent(face, f -> new ArrayList<>()).add(stack); return Stack.EMPTY; }
    @Override public int ejectFluid(Tank tank, int amount) { return 0; }
    @Override public ContainerResult processContainer(Stack container, Tank tank) { return null; }
    @Override public List<ItemEntityView> itemsAt(List<int[]> offsets) { return List.of(); }
    @Override public List<Face> shuffledFaces() { return List.of(Face.values()); }
    @Override public void affectLiving(List<int[]> offsets, Function<FishEffects.Living, List<FishEffects.Action>> rule) {}
    @Override public void affectWorld(FishEffects.WorldAction action, List<int[]> offsets) { worldActions.add(action); }

    /** Advances one tick and runs the machine. */
    public void tick(Machine m) {
        m.tick(this);
        time++;
    }
}

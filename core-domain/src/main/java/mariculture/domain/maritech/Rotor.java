package mariculture.domain.maritech;

import java.util.List;
import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.factory.Geyser;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;

/**
 * Copper, aluminium and titanium rotors (maritech/tile/TileRotor.java, TileRotorCopper/Aluminum/Titanium.java,
 * items/ItemRotor.java, 1.3.0). The rotor item turns a base iron block into a rotor (ItemRotor.onItemUse:27). A sluice
 * or gas turbine hands a rotor energy; the rotor multiplies it by its tier (1, 2, 5) and gives it to the generator at
 * the end of its straight line of rotors (addEnergy:86, TileGenerator.reset:251), then wears by the damage it was
 * given. Worn out (1440000, 8640000, 34560000) it turns back into base iron. Broken, it leaves base iron and drops the
 * rotor item with its wear (ExtensionRenderedMachine.onBlockBroken:137). While it spins (360 ticks after energy) it
 * hurts what touches it for 1.25, 2 or 5 (BlockRenderedMachine.onEntityCollidedWithBlock:131).
 *
 * <p>Fixed: the placed rotor's setDamage ignored the item's wear and started new (TileRotorCopper.setDamage:16, B-005);
 * here the item keeps the block's wear in its data and the block takes it back. Changed: the rotor finds its
 * generator when it gets energy instead of caching a master; the blades are a still model; a broken rotor drops the
 * base iron as an item instead of leaving the block.
 */
public final class Rotor extends Machine {
    public record Tier(String item, String block, int multiplier, int maxWear, float contactDamage) {}

    public static final Tier COPPER = new Tier("copper_rotor", "copper_rotor_block", 1, 1440000, 1.25f);
    public static final Tier ALUMINUM = new Tier("aluminum_rotor", "aluminum_rotor_block", 2, 8640000, 2f);
    public static final Tier TITANIUM = new Tier("titanium_rotor", "titanium_rotor_block", 5, 34560000, 5f);
    public static final List<Tier> TIERS = List.of(COPPER, ALUMINUM, TITANIUM);
    public static final MachineType COPPER_TYPE = type(COPPER), ALUMINUM_TYPE = type(ALUMINUM), TITANIUM_TYPE = type(TITANIUM);
    public static final String WEAR = "Wear", BASE = "mariculture:base_iron";
    /** TileRotor.updateEntity: isAnimating counts to 360. */
    public static final int SPIN_TICKS = 360;
    /** How far a rotor looks along its line for the generator. */
    public static final int MAX_LINE = 64;
    private static final int[] NONE = {};

    private final Tier tier;
    private final MachineType type;
    private int wear;
    private int spin;
    private boolean wornOut;

    private static MachineType type(Tier tier) {
        MachineType[] self = new MachineType[1];
        self[0] = new MachineType(tier.block(), () -> new Rotor(tier, self[0]), null);
        return self[0];
    }

    private Rotor(Tier tier, MachineType type) {
        this.tier = tier;
        this.type = type;
    }

    public static Tier tierOfItem(String item) {
        for (Tier t : TIERS) if (("mariculture:" + t.item()).equals(item)) return t;
        return null;
    }

    public static Tier tierOfBlock(String block) {
        for (Tier t : TIERS) if (("mariculture:" + t.block()).equals(block)) return t;
        return null;
    }

    public Tier tier() {
        return tier;
    }

    @Override
    public MachineType type() {
        return type;
    }

    @Override
    public int slotCount() {
        return 0;
    }

    @Override
    public int[] slots(Face side) {
        return NONE;
    }

    public int wear() {
        return wear;
    }

    public boolean spinning() {
        return spin > 0;
    }

    /** The rotor item's wear; a new item has none. */
    public static int wear(Stack item) {
        return Math.max(0, item.intData(WEAR, 0));
    }

    /** The rotor item for a tier with this wear. */
    public static Stack item(Tier tier, int wear) {
        Stack s = Stack.of("mariculture:" + tier.item());
        return wear > 0 ? s.withData(Map.of(WEAR, wear)) : s;
    }

    /** Durability bar width (0-13) of a rotor item. */
    public static int bar(Stack item) {
        Tier t = tierOfItem(item.item());
        if (t == null) return 13;
        return Math.round(13f * Math.max(0, t.maxWear() - wear(item)) / t.maxWear());
    }

    /** Placing an item on base iron: the new block takes the item's wear. */
    public void placedFrom(Stack item) {
        wear = Math.min(tier.maxWear() - 1, wear(item));
    }

    @Override
    public void tick(MachineWorld world) {
        if (spin <= 0) return;
        spin--;
        // Entities touching the rotor block while it spins.
        world.hurtLiving(new double[] {-0.1, -0.1, -0.1, 1.1, 1.1, 1.1}, tier.contactDamage());
    }

    /**
     * Energy and wear from a sluice or gas turbine for the rotor at an offset from the caller: the generator at the end
     * of the rotor's line gets {@code energy} times the tier; the rotor wears by {@code damage}. Returns the energy the
     * generator passed on, or -1 when there is no rotor there.
     */
    public static int power(MachineWorld world, int rx, int ry, int rz, int energy, int damage) {
        if (!(world.machine(rx, ry, rz) instanceof Rotor rotor)) return -1;
        int given = 0;
        int[] gen = generator(world, rx, ry, rz);
        if (gen != null && energy > 0) given = Generator.output(world, gen[0], gen[1], gen[2], energy * rotor.tier.multiplier());
        rotor.spin = SPIN_TICKS;
        rotor.wear += Math.max(0, damage);
        if (rotor.wear >= rotor.tier.maxWear()) {
            rotor.wornOut = true;
            world.setBlock(rx, ry, rz, BASE);
        }
        rotor.slotChanged(-1);
        return given;
    }

    /**
     * The generator a rotor at an offset feeds: along each horizontal way, past a straight line of rotors, a generator
     * whose front points away from them (TileGenerator.reset: rotors sit behind the generator). Null when none.
     */
    public static int[] generator(MachineWorld world, int rx, int ry, int rz) {
        for (Face d : List.of(Face.NORTH, Face.SOUTH, Face.WEST, Face.EAST)) {
            int[] o = Geyser.offset(d);
            for (int k = 1; k <= MAX_LINE; k++) {
                int x = rx + o[0] * k, z = rz + o[2] * k;
                Machine m = world.machine(x, ry, z);
                if (m instanceof Rotor) continue;
                if (m instanceof Generator && world.facingAt(x, ry, z) == d) return new int[] {x, ry, z};
                break;
            }
        }
        return null;
    }

    @Override
    public void removed(MachineWorld world) {
        if (wornOut) return;
        world.drop(item(tier, wear));
        world.drop(Stack.of(BASE));
    }

    @Override
    public void save(Map<String, Object> out) {
        super.save(out);
        out.put(WEAR, wear);
        out.put("Spin", spin);
    }

    @Override
    public void load(Map<String, ?> in) {
        super.load(in);
        wear = Math.max(0, integer(in.get(WEAR), 0));
        spin = Math.max(0, integer(in.get("Spin"), 0));
    }

    @Override
    public void describe(List<mariculture.domain.view.RecipeViews.Line> out) {
        out.add(new mariculture.domain.view.RecipeViews.Line("jade.mariculture.rotor_wear", wear, tier.maxWear()));
    }
}

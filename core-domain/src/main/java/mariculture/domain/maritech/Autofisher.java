package mariculture.domain.maritech;

import mariculture.domain.machine.SlotInfo;

import mariculture.domain.content.Catalog;
import mariculture.domain.content.Face;
import mariculture.domain.content.ItemKind;
import mariculture.domain.content.ItemSpec;
import mariculture.domain.fish.LiveFish;
import mariculture.domain.fish.Species;
import mariculture.domain.fishery.Bait;
import mariculture.domain.fishing.FishingRules;
import mariculture.domain.machine.EjectSetting;
import mariculture.domain.machine.Layout;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;

/**
 * Autofisher (maritech/tile/TileAutofisher.java, 1.3.0): fishes the water below (lava in the Nether, BlockHelper
 * .isFishable:40) with the rod in slot 4 and bait from slots 5-10; catches go to slots 11-19. Progress is the speed
 * upgrade total x 4 (updateUpgrades:39) against 2500 (ExtensionMachines:22). Each completion casts 1 + Lure times
 * (process:68); a cast succeeds when 0-99 rolls under the bait quality + Luck of the Sea x 4, then rolls the catch as
 * a player's rod would (FishingRules.cast), wears the rod by one (RodType.damage:88) and uses one bait when something
 * was caught. 20000 FE base; a working tick uses (20 + speed x 20) x the RF modifier, 1 above 300000 RF upgrades
 * (updatePowerPerTick:98).
 */
public final class Autofisher extends PoweredMachine {
    public static final int MAX = 2500, ROD = 4, BAIT = 5, OUT = 11, SLOTS = 20;
    public static final int FLUX_CAST = 100;
    public static final String LURE = "minecraft:lure", LUCK = "minecraft:luck_of_the_sea";
    private static final int[] ROD_SLOT = {ROD}, BAITS = range(BAIT, 6), OUTPUTS = range(OUT, 9), SIDES = range(BAIT, 15);

    public static final MachineType TYPE = new MachineType("autofisher", Autofisher::new, Layout.builder(176)
            .slot(ROD, 26, 17)
            .grid(BAIT, 3, 2, 26, 35)
            .progress(G_PROGRESS, G_MAX, 84, 35, 18, 8, "right", 0xFF4A90E2)
            .grid(OUT, 3, 3, 106, 17)
            .slot(BATTERY_SLOT, 8, 53)
            .progress(G_EXTRA, G_EXTRA + 1, 10, 17, 4, 32, "up", 0xFFCC2222)
            .label("gui.mariculture.energy", G_EXTRA, 8, 78)
            .standardSidePanel(true, true)
            .build());

    private static int[] range(int first, int n) {
        int[] r = new int[n];
        for (int i = 0; i < n; i++) r[i] = first + i;
        return r;
    }

    public Autofisher() {
        super(20000);
        eject = EjectSetting.ITEM;
    }

    @Override
    public MachineType type() {
        return TYPE;
    }

    @Override
    public int slotCount() {
        return SLOTS;
    }

    @Override
    protected int max() {
        return mariculture.domain.config.Settings.AUTOFISHER_SPEED.get();
    }

    @Override
    protected int[] outputs() {
        return OUTPUTS;
    }

    @Override
    protected EjectSetting ejectType() {
        return EjectSetting.ITEM;
    }

    @Override
    protected int speed() {
        return upgrades.speed() * 4;
    }

    @Override
    public int usage() {
        if (upgrades.rf() > 300000) return 1;
        return (int) (rfModifier() * (20 + speed() * 20));
    }

    /** The rod tier of an item: Mariculture rods by their catalog tier, the vanilla rod as DIRE; null otherwise. */
    public static Species.Rod rod(String item) {
        if (item.equals("minecraft:fishing_rod")) return Species.Rod.DIRE;
        for (ItemSpec s : Catalog.items()) {
            if (s.kind() instanceof ItemKind.Rod r && item.equals("mariculture:" + s.id())) return r.tier();
        }
        return mariculture.domain.plugin.Plugins.rodTier(item);
    }

    @Override
    protected boolean validStack(int slot, Stack stack) {
        if (slot == ROD) return rod(stack.item()) != null;
        if (slot >= BAIT && slot < OUT) return Bait.quality(stack.item()) > 0;
        if (slot >= OUT) return false;
        return super.validStack(slot, stack);
    }

    @Override
    public int slotLimit(int slot) {
        return slot == ROD ? 1 : super.slotLimit(slot);
    }

    @Override
    public int[] slots(Face side) {
        return side == Face.UP || side == Face.DOWN ? ROD_SLOT : SIDES;
    }

    @Override
    public boolean canInsert(int slot, Stack stack, Face side) {
        return (slot == ROD || slot >= BAIT && slot < OUT) && validStack(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, Face side) {
        return slot == ROD || slot >= OUT;
    }

    /** The rod's tier while it can fish: a flux rod needs 100 FE (RodType.FLUX.canFish). */
    private Species.Rod tier() {
        Stack r = inv.get(ROD);
        if (r.isEmpty()) return null;
        if (EnergyItem.spec(r) != null && EnergyItem.stored(r) < FLUX_CAST) return null;
        return rod(r.item());
    }

    /** RodType.damage: one durability, or 100 FE from a flux rod. */
    private void wearRod() {
        Stack r = inv.get(ROD);
        if (EnergyItem.spec(r) != null) inv.setData(ROD, EnergyItem.extract(r, FLUX_CAST, false).stack().data());
        else inv.damage(ROD, 1);
    }

    /** getBait:119: the first bait slot the rod may use, or -1. */
    private int baitSlot(Species.Rod tier) {
        for (int s : BAITS) {
            Stack b = inv.get(s);
            if (!b.isEmpty() && Bait.canUse(tier, b.item())) return s;
        }
        return -1;
    }

    private static boolean fishable(MachineWorld world) {
        String f = world.fluid(0, -1, 0);
        return f.equals("minecraft:water") || f.equals("minecraft:lava") && world.isNether();
    }

    @Override
    protected boolean canWork(MachineWorld world) {
        Species.Rod tier = tier();
        return tier != null && baitSlot(tier) >= 0 && hasPower(1) && fishable(world) && hasRoom();
    }

    @Override
    protected void process(MachineWorld world) {
        int casts = 1 + inv.enchantLevel(ROD, LURE);
        for (int i = 0; i < casts; i++) {
            Species.Rod tier = tier();
            if (tier == null) return;
            int bait = baitSlot(tier);
            if (bait < 0) return;
            int luck = inv.enchantLevel(ROD, LUCK);
            int chance = Bait.quality(inv.get(bait).item()) + luck * 4;
            if (world.random().nextInt(100) >= chance) continue;
            FishingRules.Cast cast = world.castBelow(tier, luck);
            if (cast == null || LiveFish.registry().allSpecies().isEmpty()) return;
            FishingRules.Catch c = FishingRules.cast(cast, LiveFish.registry(), world.random());
            wearRod();
            if (world.deliverCatch(c, ROD, OUTPUTS, eject.ejectsItems())) inv.shrink(bait, 1);
        }
    }

    @Override
    public int[] gauges() {
        return gauges(battery.stored(), battery.capacity());
    }

    @Override
    protected SlotInfo info(int slot) {
        if (slot == ROD) {
            java.util.List<String> g = new java.util.ArrayList<>();
            g.add("minecraft:fishing_rod");
            for (ItemSpec s : Catalog.items()) if (s.kind() instanceof ItemKind.Rod) g.add("mariculture:" + s.id());
            return SlotInfo.of(SlotInfo.Role.CATALYST, "rod", g, s -> rod(s.item()) != null);
        }
        if (slot >= BAIT && slot < OUT) {
            java.util.List<String> g = new java.util.ArrayList<>(Bait.QUALITY.keySet());
            g.sort(java.util.Comparator.comparingInt(Bait::quality));
            return SlotInfo.input("bait", g, s -> Bait.quality(s.item()) > 0);
        }
        return super.info(slot);
    }
}

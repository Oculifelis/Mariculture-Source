package mariculture.domain.fishery;

import mariculture.domain.machine.SlotInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mariculture.domain.content.Face;
import mariculture.domain.fish.Boosters;
import mariculture.domain.fish.Breeding;
import mariculture.domain.fish.BreedingCondition;
import mariculture.domain.fish.Egg;
import mariculture.domain.fish.Environment;
import mariculture.domain.fish.FishAbility;
import mariculture.domain.fish.FishEffects;
import mariculture.domain.fish.Genes;
import mariculture.domain.fish.Genome;
import mariculture.domain.fish.LiveFish;
import mariculture.domain.fish.Salinity;
import mariculture.domain.fish.Species;
import mariculture.domain.fluid.Fluids;
import mariculture.domain.machine.EjectSetting;
import mariculture.domain.machine.Layout;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;
import mariculture.domain.machine.StandardMachine;
import mariculture.domain.machine.Tank;

/**
 * Fish Feeder (fishery/tile/TileFeeder, 1.3.0). Slots: 0-2 upgrades, 3 fluid/food in, 4 container out,
 * 5 male, 6 female, 7-12 output. A male and a female that can live in the surrounding water (scanned in an
 * 11x11x11 box widened by the male's area genes) eat fish food, produce, age and finally die, leaving their raw
 * form and, for the female, an egg.
 *
 * <p>Changes from legacy: fish food items put in slot 3 become fish food fluid here only (BUG-002: legacy did
 * this in every machine's container slot); the tank holds fish food only; the tank volume is rescanned when a
 * fish changes and every 200 ticks rather than only at load (BUG-028 asks for change-driven rescans). Like
 * legacy, the tank is only resized while both fish are present, so removing a fish keeps the food (BUG-018).
 * The fish's maximum life is read from the stack, so the "fix fish" pass for pre-1.3 stacks (BUG-027) is
 * unnecessary.
 */
public final class FishFeeder extends StandardMachine {
    public static final int FLUID_IN = 3;
    public static final int FLUID_OUT = 4;
    public static final int MALE = 5;
    public static final int FEMALE = 6;
    private static final int[] OUT = {7, 8, 9, 10, 11, 12};
    /** core/config/Machines.java: feeder speed 200, effect tick 20, fish food tick 25 s. */
    public static final int SPEED = 200;
    public static final int EFFECT_TICK = 20;
    public static final int FOOD_TICK = 25 * 20;
    public static final int G_TANK_SIZE = G_EXTRA;
    public static final int G_TEMPERATURE = G_EXTRA + 1;
    public static final int G_SALINITY = G_EXTRA + 2;
    public static final int G_MALE_LIFE = G_EXTRA + 3;
    public static final int G_FEMALE_LIFE = G_EXTRA + 4;
    public static final int G_PROBLEMS = G_EXTRA + 5;

    public static final MachineType TYPE = new MachineType("fish_feeder", FishFeeder::new, Layout.builder(176)
            .tank(0, 33, 19, 34, 54)
            .slot(FLUID_IN, 12, 19).slot(FLUID_OUT, 12, 55)
            .slot(MALE, 75, 24).slot(FEMALE, 75, 52)
            .progressFixed(G_MALE_LIFE, 100, 95, 24, 4, 18, "up", 0xFF33AA33)
            .progressFixed(G_FEMALE_LIFE, 100, 95, 52, 4, 18, "up", 0xFF33AA33)
            .grid(7, 3, 2, 116, 24)
            .label("gui.mariculture.feeder.tank_size", G_TANK_SIZE, 8, 78)
            .label("gui.mariculture.feeder.temperature", G_TEMPERATURE, 70, 78)
            .standardSidePanel(true, true)
            .build());

    private final Tank tank = new Tank(0, f -> f.equals(Fluids.FISH_FOOD));
    private final List<int[]> coords = new ArrayList<>();
    private String tankFluid;
    private boolean swap;
    private int foodTick;
    /** UUID of the player who placed the feeder, for research breeding conditions; null when unknown. */
    private String owner;
    private boolean rescan = true;
    private int temperature;
    private Salinity salinity = Salinity.FRESH;
    private int problems;
    /** Whether the pair's species breeding needs held at the last check (problem bit 7 when not). */
    private boolean breedingOk = true;

    public FishFeeder() {
        tanks.add(tank);
    }

    @Override
    public MachineType type() {
        return TYPE;
    }

    @Override
    public int slotCount() {
        return 13;
    }

    @Override
    protected int max() {
        return mariculture.domain.config.Settings.FEEDER_SPEED.get();
    }

    @Override
    protected int[] outputs() {
        return OUT;
    }

    @Override
    protected EjectSetting ejectType() {
        return EjectSetting.ITEM;
    }

    @Override
    protected boolean validStack(int slot, Stack stack) {
        if (slot == MALE) return LiveFish.isMale(stack);
        if (slot == FEMALE) return LiveFish.isFemale(stack);
        if (slot == FLUID_IN) return true;
        return false;
    }

    @Override
    public int slotLimit(int slot) {
        return slot == MALE || slot == FEMALE ? 1 : super.slotLimit(slot);
    }

    @Override
    public int[] slots(Face side) {
        return new int[] {MALE, FEMALE, FLUID_IN, 7, 8, 9, 10, 11, 12};
    }

    @Override
    public boolean canInsert(int slot, Stack stack, Face side) {
        if (slot == MALE || slot == FEMALE) return inv.get(slot).isEmpty() && validStack(slot, stack);
        return slot == FLUID_IN;
    }

    @Override
    public boolean canExtract(int slot, Face side) {
        return slot > FEMALE;
    }

    @Override
    public void slotChanged(int slot) {
        super.slotChanged(slot);
        if (slot == MALE || slot == FEMALE) rescan = true;
    }

    /** Tank capacity 2 * tank size * (storage + 1) (TileFeeder.getTankCapacity). */
    @Override
    protected void upgradesChanged() {
        if (!coords.isEmpty()) tank.setCapacity(2 * coords.size() * (upgrades.storage() + 1));
    }

    @Override
    protected void update(MachineWorld world) {
        if (rescan || world.gameTime() % 200 == 0) {
            rescan = false;
            scan(world);
        }
        if (world.gameTime() % 20 == 0) {
            world.guardSpawns(guardBox());
            breedingOk = pairBreedingOk(world);
        }
        feedFromSlot(world);
        temperature = world.temperature() + upgrades.temperature();
        salinity = world.salinity().shift(upgrades.salinity());
        if (canWork) {
            foodTick++;
            if (world.gameTime() % EFFECT_TICK == 0) {
                doEffect(world, swap ? MALE : FEMALE);
                swap = !swap;
            }
            if (foodTick % FOOD_TICK == 0) useFood(world, swap ? MALE : FEMALE);
        }
    }

    /** Fish food items become fish food fluid; real fluid containers go through the loader's fluid item API. */
    private void feedFromSlot(MachineWorld world) {
        Stack in = inv.get(FLUID_IN);
        if (in.isEmpty()) return;
        int value = Bait.fishFood(in.item());
        if (value > 0) {
            if (tank.fill(Fluids.FISH_FOOD, value, true) == value) {
                tank.fill(Fluids.FISH_FOOD, value, false);
                inv.shrink(FLUID_IN, 1);
                markDirty();
            }
            return;
        }
        if (world.gameTime() % 20 != 0 || !inv.get(FLUID_OUT).isEmpty()) return;
        MachineWorld.ContainerResult r = world.processContainer(in, tank);
        if (r != null) {
            inv.shrink(FLUID_IN, 1);
            Stack left = inv.insert(r.container(), new int[] {FLUID_OUT}, false);
            if (!left.isEmpty()) world.drop(left);
            markDirty();
        }
    }

    /**
     * TileFeeder.updateTankSize: water blocks around the feeder in a box of +-5 widened by the male's area genes,
     * that both fish accept; the most common fluid is the tank fluid.
     */
    private void scan(MachineWorld world) {
        coords.clear();
        tankFluid = null;
        Genome m = LiveFish.genome(inv.get(MALE)), f = LiveFish.genome(inv.get(FEMALE));
        Species sm = m == null ? null : LiveFish.registry().species(m.species());
        Species sf = f == null ? null : LiveFish.registry().species(f.species());
        if (sm != null && sf != null && m.isMale() && f.isFemale()) {
            int xP = m.upper(Genes.AREA_EAST), xN = m.upper(Genes.AREA_WEST), yP = m.upper(Genes.AREA_UP);
            int yN = m.upper(Genes.AREA_DOWN), zP = m.upper(Genes.AREA_SOUTH), zN = m.upper(Genes.AREA_NORTH);
            Map<String, Integer> count = new HashMap<>();
            for (int x = -5 - xN; x <= 5 + xP; x++) {
                for (int z = -5 - zN; z <= 5 + zP; z++) {
                    for (int y = -5 - yN; y <= 5 + yP; y++) {
                        String fluid = world.fluid(x, y, z);
                        if (fluid.isEmpty() || !sm.validFluids().contains(fluid) || !sf.validFluids().contains(fluid)) continue;
                        coords.add(new int[] {x, y, z});
                        count.merge(fluid, 1, Integer::sum);
                    }
                }
            }
            int best = 0;
            for (Map.Entry<String, Integer> e : count.entrySet()) {
                if (e.getValue() >= best) {
                    best = e.getValue();
                    tankFluid = e.getKey();
                }
            }
            // Legacy only resized with both fish present, so taking a fish out never empties the food (BUG-018).
            tank.setCapacity(2 * coords.size() * (upgrades.storage() + 1));
        }
        markDirty();
    }

    @Override
    protected boolean canWork(MachineWorld world) {
        problems = 0;
        boolean ok = canLive(world, MALE, true) & canLive(world, FEMALE, false);
        if (!hasRoom()) problems |= 1 << 6;
        if (!breedingOk) problems |= 1 << 7;
        return ok && hasRoom();
    }

    /** Each fish's species and life, then why the pair is not working (problem bits from canWork). */
    @Override
    public void describe(List<mariculture.domain.view.RecipeViews.Line> out) {
        super.describe(out);
        for (int slot : new int[] {MALE, FEMALE}) {
            Genome g = LiveFish.genome(inv.get(slot));
            if (g == null) continue;
            out.add(new mariculture.domain.view.RecipeViews.Line(slot == MALE ? "jade.mariculture.feeder.male" : "jade.mariculture.feeder.female",
                    "lang:" + mariculture.domain.fish.FishText.speciesKey(g.species()), lifePercent(slot)));
        }
        for (int bit = 0; bit < 8; bit++) {
            if ((problems & (1 << bit)) != 0) out.add(new mariculture.domain.view.RecipeViews.Line("jade.mariculture.feeder.problem." + bit));
        }
    }

    /** A Stargazer in either slot guards the tank's surroundings from hostile spawns (see {@link SpawnGuard}). */
    int[] guardBox() {
        if (!mariculture.domain.config.Settings.STARGAZER_BLOCKS_SPAWNS.get()) return null;
        boolean stargazer = false;
        for (int slot : new int[] {MALE, FEMALE}) {
            Genome g = LiveFish.genome(inv.get(slot));
            if (g != null && "mariculture:stargazer".equals(g.species())) stargazer = true;
        }
        return stargazer ? SpawnGuard.around(coords) : null;
    }

    @Override
    public void removed(MachineWorld world) {
        super.removed(world);
        world.guardSpawns(null);
    }

    /** TileFeeder.fishCanLive plus FishyHelper.canLive; sets problem bits for the screen. */
    private boolean canLive(MachineWorld world, int slot, boolean male) {
        Stack s = inv.get(slot);
        Genome g = LiveFish.genome(s);
        Species sp = g == null ? null : LiveFish.registry().species(g.species());
        if (g == null || sp == null || g.isMale() != male) {
            problems |= male ? 1 : 2;
            return false;
        }
        if (upgrades.has("debugLive")) return true; // TileFeeder.java:459
        boolean ok = true;
        if (!tank.contains(Fluids.FISH_FOOD)) { problems |= 1 << 2; ok = false; }
        if (coords.size() < g.expressed(Genes.WATER_REQUIRED, Boosters.LEGACY_DEFAULTS) || tankFluid == null || !sp.validFluids().contains(tankFluid)) {
            problems |= 1 << 3;
            ok = false;
        }
        if (!sp.activeTime().allows(world.isDay())) { problems |= 1 << 4; ok = false; }
        Salinity salt = salinity;
        int temp = temperature;
        if (upgrades.has("aquascum")) {
            salt = sp.salinityBase();
            temp = sp.temperatureBase();
        }
        if (!Environment.canLive(g, sp, salt, temp, Boosters.LEGACY_DEFAULTS)) { problems |= 1 << 5; ok = false; }
        return ok;
    }

    @Override
    protected void process(MachineWorld world) {
        makeProduct(world, swap ? FEMALE : MALE);
        damageFish(world, FEMALE);
        damageFish(world, MALE);
    }

    /** TileFeeder.makeProduct: one product roll per point of work ethic. */
    private void makeProduct(MachineWorld world, int slot) {
        Genome g = LiveFish.genome(inv.get(slot));
        Species sp = g == null ? null : LiveFish.registry().species(g.species());
        if (sp == null) return;
        int rolls = g.expressed(Genes.PRODUCTIVITY, Boosters.LEGACY_DEFAULTS);
        for (int i = 0; i < rolls; i++) {
            insertOutput(world, product(sp, world));
            int bonus = percent(sp, FishAbility.ProductBonus.class);
            if (bonus > 0 && world.random().nextInt(100) < bonus) insertOutput(world, product(sp, world));
            if (upgrades.has("female")) {
                int fertility = Math.max(1, (5500 - g.expressed(Genes.FERTILITY, Boosters.LEGACY_DEFAULTS)) / 50);
                if (world.random().nextInt(fertility) == 0) generateEgg(world);
            }
            if (upgrades.has("male")) insertOutput(world, product(sp, world));
        }
    }

    /** FishSpecies.getProduct: the first product in list order whose per-mille roll passes. */
    private static Stack product(Species sp, MachineWorld world) {
        for (Species.Product p : sp.products()) {
            if (world.random().nextInt(1000) < p.rollPerMille()) {
                String item = p.item().equals("self:dead_fish") ? mariculture.domain.fishing.FishingRules.rawForm(sp.id()) : p.item();
                return Stack.of(item, p.count(), p.data());
            }
        }
        return Stack.EMPTY;
    }

    /** TileFeeder.damageFish: life drops by (speed - purity * 15) per cycle unless an eternal upgrade protects it. */
    private void damageFish(MachineWorld world, int slot) {
        Stack s = inv.get(slot);
        Genome g = LiveFish.genome(s);
        Species sp = g == null ? null : LiveFish.registry().species(g.species());
        if (sp == null) return;
        if (g.isFemale() && upgrades.has("female")) return;
        if (g.isMale() && upgrades.has("male")) return;
        int loss = mariculture.domain.config.Settings.FEEDER_SPEED.get() - upgrades.purity() * 15;
        loss = loss * (100 - percent(sp, FishAbility.Longevity.class)) / 100;
        int life = LiveFish.currentLife(s) - loss;
        if (life <= 0 || upgrades.has("debugKill")) { // TileFeeder.java:242
            insertOutput(world, Stack.of(mariculture.domain.fishing.FishingRules.rawForm(sp.id())));
            if (g.isFemale()) generateEgg(world);
            inv.set(slot, Stack.EMPTY);
            rescan = true;
        } else {
            inv.set(slot, LiveFish.withLife(s, life));
        }
    }

    private void generateEgg(MachineWorld world) {
        Genome m = LiveFish.genome(inv.get(MALE)), f = LiveFish.genome(inv.get(FEMALE));
        if (m == null || f == null) return;
        BreedingCondition.Context ctx = breedingContext(world);
        List<BreedingCondition> needs = pairNeeds(m, f);
        if (!BreedingCondition.all(needs, ctx)) return;
        eatFood(world, needs);
        Egg egg = tagEgg(world, Breeding.layEgg(m, f, Boosters.LEGACY_DEFAULTS));
        insertOutput(world, LiveFish.eggStack(tagBred(world, egg, ctx)));
    }

    /** Both parents' species breeding needs. */
    private static List<BreedingCondition> pairNeeds(Genome m, Genome f) {
        List<BreedingCondition> out = new ArrayList<>();
        Species sm = LiveFish.registry().species(m.species()), sf = LiveFish.registry().species(f.species());
        if (sm != null) out.addAll(sm.breeding());
        if (sf != null && sf != sm) out.addAll(sf.breeding());
        return out;
    }

    private boolean pairBreedingOk(MachineWorld world) {
        Genome m = LiveFish.genome(inv.get(MALE)), f = LiveFish.genome(inv.get(FEMALE));
        if (m == null || f == null) return true;
        List<BreedingCondition> needs = pairNeeds(m, f);
        return needs.isEmpty() || BreedingCondition.all(needs, breedingContext(world));
    }

    /**
     * Mutations with breeding conditions: when the egg carries both parent species and the conditions hold now, the
     * egg gets the mutation's lay tag (checked again at hatching, see {@link mariculture.domain.fish.Mutation}).
     */
    private Egg tagBred(MachineWorld world, Egg egg, BreedingCondition.Context ctx) {
        java.util.Set<String> species = new java.util.HashSet<>();
        for (int i = 0; i < 4; i++) species.add(egg.species(i));
        for (mariculture.domain.fish.Mutation mu : LiveFish.registry().mutations()) {
            if (mu.breeding().isEmpty() || !species.contains(mu.parentA()) || !species.contains(mu.parentB())) continue;
            if (!BreedingCondition.all(mu.breeding(), ctx)) continue;
            egg = egg.withTag(mu.layTag());
            eatFood(world, mu.breeding());
        }
        return egg;
    }

    /** One item from the food slot per food need. */
    private void eatFood(MachineWorld world, List<BreedingCondition> needs) {
        for (BreedingCondition b : needs) {
            if (b instanceof BreedingCondition.Food food && foodMatches(world, food.itemOrTag())) {
                inv.shrink(FLUID_IN, 1);
                markDirty();
            }
        }
    }

    private boolean foodMatches(MachineWorld world, String itemOrTag) {
        Stack in = inv.get(FLUID_IN);
        if (in.isEmpty()) return false;
        return itemOrTag.startsWith("#") ? world.itemHasTag(in.item(), itemOrTag.substring(1)) : in.item().equals(itemOrTag);
    }

    private BreedingCondition.Context breedingContext(MachineWorld world) {
        return new BreedingCondition.Context() {
            private int light = -1;

            @Override public int light() {
                if (light < 0) light = tankLight(world);
                return light;
            }

            @Override public int height() { return world.y(); }

            @Override public int water() { return coords.size(); }

            @Override public Salinity salinity() { return salinity; }

            @Override public int temperature() { return temperature; }

            @Override public boolean raining() { return world.isRaining(); }

            @Override public boolean hasFood(String itemOrTag) { return foodMatches(world, itemOrTag); }

            @Override public String tankFluid() { return tankFluid; }

            @Override public boolean researchDone(String entry) { return owner != null && world.researchDone(owner, entry); }
        };
    }

    /** The brightest light in the tank water, sampling at most about 64 blocks. */
    private int tankLight(MachineWorld world) {
        int best = 0, step = Math.max(1, coords.size() / 64);
        for (int i = 0; i < coords.size() && best < 15; i += step) {
            int[] c = coords.get(i);
            best = Math.max(best, world.light(c[0], c[1], c[2]));
        }
        return best;
    }

    /** The sum of this species' percent abilities of one kind (product bonus, longevity, food saver), at most 100. */
    static int percent(Species sp, Class<? extends FishAbility> kind) {
        int total = 0;
        for (FishAbility a : sp.abilities()) {
            if (!kind.isInstance(a)) continue;
            if (a instanceof FishAbility.ProductBonus b) total += b.chancePercent();
            else if (a instanceof FishAbility.Longevity l) total += l.percent();
            else if (a instanceof FishAbility.FoodSaver fs) total += fs.percent();
        }
        return Math.min(100, total);
    }

    /**
     * MetalFishEventHandler.onGenerateEgg: every block face touching the tank water that is a storage block a
     * mutation asks for adds one to that tag on the egg (legacy counted "block*" ore names on all six neighbours
     * of every water block). Only tags some mutation needs are checked.
     */
    private Egg tagEgg(MachineWorld world, Egg egg) {
        java.util.Set<String> wanted = new java.util.LinkedHashSet<>();
        for (mariculture.domain.fish.Mutation mu : LiveFish.registry().mutations()) wanted.addAll(mu.eggTags());
        if (wanted.isEmpty()) return egg;
        int[][] dirs = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
        for (int[] c : coords) {
            for (int[] d : dirs) {
                int x = c[0] + d[0], y = c[1] + d[1], z = c[2] + d[2];
                if (!world.fluid(x, y, z).isEmpty() || world.isAir(x, y, z)) continue;
                for (String tag : wanted) if (world.blockHasTag(x, y, z, tag)) egg = egg.withTag(tag);
            }
        }
        return egg;
    }

    /** TileFeeder.doEffect: the species' world effect, then its living effect on everything standing in the tank water. */
    private void doEffect(MachineWorld world, int slot) {
        Genome g = LiveFish.genome(inv.get(slot));
        Species sp = g == null ? null : LiveFish.registry().species(g.species());
        if (sp == null) return;
        for (FishEffects.WorldAction a : FishEffects.world(sp.id(), world.random(), coords.size())) world.affectWorld(a, coords);
        if (FishEffects.hasLivingEffect(sp.id())) {
            boolean day = world.isDay();
            world.affectLiving(coords, e -> FishEffects.living(sp.id(), e, day, world.random()));
        }
        applyAbilities(world, sp);
    }

    /** The species' area abilities ({@link FishAbility.AreaAbility}) around the tank water. */
    private void applyAbilities(MachineWorld world, Species sp) {
        if (sp.abilities().isEmpty() || coords.isEmpty()) return;
        boolean day = world.isDay();
        for (FishAbility a : sp.abilities()) {
            if (a instanceof FishAbility.VisRefill v) {
                if (world.random().nextInt(100) < v.chancePercent()) world.addAuraVis(v.amount());
                continue;
            }
            if (a instanceof FishAbility.FluxEat f) {
                if (world.random().nextInt(100) < f.chancePercent()) world.drainAuraFlux(f.amount());
                continue;
            }
            if (!(a instanceof FishAbility.AreaAbility area)) continue;
            FishAbility.Area ar = area.area();
            if (!ar.when().allows(day) || world.random().nextInt(100) >= ar.chancePercent()) continue;
            if (a instanceof FishAbility.GrowPlants g) growPlants(world, ar.radius(), g.tries());
            else world.affectLivingNear(coords, ar.radius(), area::actions);
        }
    }

    /** Bone meal on random crops or saplings within the tank water's bounds grown by {@code radius}. */
    private void growPlants(MachineWorld world, int radius, int tries) {
        int[] min = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        int[] max = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (int[] c : coords) {
            for (int i = 0; i < 3; i++) {
                min[i] = Math.min(min[i], c[i] - radius);
                max[i] = Math.max(max[i], c[i] + radius);
            }
        }
        for (int t = 0; t < tries; t++) {
            int x = min[0] + world.random().nextInt(max[0] - min[0] + 1);
            int y = min[1] + world.random().nextInt(max[1] - min[1] + 1);
            int z = min[2] + world.random().nextInt(max[2] - min[2] + 1);
            if (world.blockHasTag(x, y, z, "minecraft:crops") || world.blockHasTag(x, y, z, "minecraft:saplings")) world.bonemeal(x, y, z);
        }
    }

    /** TileFeeder.useFood: the fish's food usage gene in mB, at least 1 when the species requires food. */
    private void useFood(MachineWorld world, int slot) {
        Genome g = LiveFish.genome(inv.get(slot));
        Species sp = g == null ? null : LiveFish.registry().species(g.species());
        if (sp == null) return;
        int saver = percent(sp, FishAbility.FoodSaver.class);
        if (saver > 0 && world.random().nextInt(100) < saver) return;
        int usage = g.expressed(Genes.FOOD_USAGE, Boosters.LEGACY_DEFAULTS);
        if (usage == 0 && sp.requiresFood()) usage = 1;
        tank.drain(Fluids.FISH_FOOD, usage, false);
        markDirty();
    }

    /** Life of a fish as 0-100 for the screen (TileFeeder.getFishLifeScaled). */
    private int lifePercent(int slot) {
        Stack s = inv.get(slot);
        int max = LiveFish.lifespan(s);
        return max <= 0 ? 0 : LiveFish.currentLife(s) * 100 / max;
    }

    @Override
    public int light() {
        int lm = speciesLight(MALE), lf = speciesLight(FEMALE);
        if (lm == 0) return lf;
        if (lf == 0) return lm;
        return (lm + lf) / 2;
    }

    private int speciesLight(int slot) {
        Species sp = LiveFish.species(inv.get(slot));
        return sp == null ? 0 : Math.max(0, sp.light());
    }

    @Override
    public int[] gauges() {
        return gauges(coords.size(), temperature, salinity.ordinal(), lifePercent(MALE), lifePercent(FEMALE), problems);
    }

    @Override
    public void save(Map<String, Object> out) {
        super.save(out);
        out.put("swap", swap);
        out.put("food_tick", foodTick);
        if (owner != null) out.put("owner", owner);
    }

    /** Records the player who placed the feeder (a UUID string). */
    public void setOwner(String uuid) {
        owner = uuid;
    }

    public String owner() {
        return owner;
    }

    @Override
    public void load(Map<String, ?> in) {
        super.load(in);
        swap = bool(in.get("swap"));
        foodTick = in.get("food_tick") instanceof Number n ? n.intValue() : 0;
        owner = in.get("owner") instanceof String o && !o.isEmpty() ? o : null;
        // Keep the saved fish food until the first scan sizes the tank.
        tank.setCapacity(Integer.MAX_VALUE);
        tank.load(in, "tank0");
        rescan = true;
    }

    @Override
    protected SlotInfo info(int slot) {
        if (slot == FLUID_IN) {
            java.util.List<String> g = new java.util.ArrayList<>(new java.util.TreeMap<>(Bait.FISH_FOOD).keySet());
            g.addAll(new java.util.TreeSet<>(mariculture.domain.plugin.Plugins.fishFoodItems()));
            g.addAll(SlotInfo.FLUID_CONTAINERS);
            return SlotInfo.of(SlotInfo.Role.CATALYST, "fish_food", g, s -> Bait.fishFood(s.item()) > 0 || SlotInfo.isFluidContainer(s));
        }
        if (slot == FLUID_OUT) return SlotInfo.OUTPUT;
        if (slot == MALE) return SlotInfo.input("male_fish", SlotInfo.liveFishItems(), LiveFish::isMale);
        if (slot == FEMALE) return SlotInfo.input("female_fish", SlotInfo.liveFishItems(), LiveFish::isFemale);
        return super.info(slot);
    }
}

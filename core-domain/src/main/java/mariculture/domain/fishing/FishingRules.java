package mariculture.domain.fishing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mariculture.domain.fish.CatchChance;
import mariculture.domain.fish.Environment;
import mariculture.domain.fish.FishRegistry;
import mariculture.domain.fish.Salinity;
import mariculture.domain.fish.Species;
import mariculture.domain.pearl.PearlColor;
import mariculture.domain.random.Rng;

/**
 * What a cast catches (fishery/FishingHandler.getCatch and getFishForLocation, 1.3.0; api/fishery/RodType,
 * Loot; fishery/FishingLoot). The 1.3.0 order is kept: rare, good, junk loot rolls by rod chance (Luck of the
 * Sea adds its own roll per rarity, BUG-006 settled on the 1.3.0 #342 semantics), then fish for the location.
 */
public final class FishingRules {
    /** Rod quality, loot chances in percent and loot enchanting level (RodType constants). NET is the fishing net block. */
    public record RodStats(int quality, double junk, double good, double rare, int enchant) {}

    public static RodStats stats(Species.Rod rod) {
        return switch (rod) {
            case DIRE -> new RodStats(10, 45, 0, 0, 5);
            case OLD -> new RodStats(25, 27.5, 5.5, 0, 10);
            case GOOD -> new RodStats(50, 10, 17.5, 1, 20);
            case SUPER -> new RodStats(75, 1, 15.5, 5, 30);
            case FLUX -> new RodStats(90, 0, 13, 7.5, 15);
            case MANA -> new RodStats(90, 10, 10, 10, 50);
            case BLOOD -> new RodStats(95, 15, 20, 5, 45);
            case INFUSED -> new RodStats(75, 1, 12, 4.5, 0);
        };
    }

    public static final RodStats NET = new RodStats(25, 0, 0, 0, 0);

    public enum Rarity { JUNK, GOOD, RARE }

    /**
     * One loot entry. {@code dimension} is "overworld_like", "nether", "end" or null for any; {@code minRod} null
     * means any rod (those entries also went into vanilla's own tables in legacy).
     */
    public record Loot(String item, double chancePercent, Rarity rarity, String dimension, Species.Rod minRod) {}

    public static final List<Loot> LOOT;

    static {
        List<Loot> l = new ArrayList<>();
        for (PearlColor c : PearlColor.values()) {
            String pearl = "mariculture:" + c.id() + "_pearl";
            l.add(new Loot(pearl, 2.5, Rarity.JUNK, "overworld_like", Species.Rod.OLD));
            l.add(new Loot(pearl, 5, Rarity.GOOD, "overworld_like", Species.Rod.OLD));
            l.add(new Loot(pearl, 2.5, Rarity.RARE, "overworld_like", Species.Rod.OLD));
        }
        for (String disc : List.of("minecraft:music_disc_far", "minecraft:music_disc_strad", "minecraft:music_disc_mall", "minecraft:music_disc_mall")) {
            l.add(new Loot(disc, 5, Rarity.GOOD, "overworld_like", Species.Rod.GOOD));
        }
        l.add(new Loot("minecraft:gold_nugget", 45, Rarity.JUNK, "nether", Species.Rod.DIRE));
        l.add(new Loot("minecraft:ghast_tear", 15, Rarity.JUNK, "nether", Species.Rod.OLD));
        for (String disc : List.of("minecraft:music_disc_13", "minecraft:music_disc_blocks", "minecraft:music_disc_chirp")) {
            l.add(new Loot(disc, 5, Rarity.GOOD, "nether", Species.Rod.GOOD));
        }
        // Potion meta 8195 is a Fire Resistance potion.
        l.add(new Loot("mariculture:potion/fire_resistance", 5, Rarity.JUNK, "nether", Species.Rod.GOOD));
        l.add(new Loot("minecraft:nether_star", 1, Rarity.RARE, "nether", Species.Rod.SUPER));
        l.add(new Loot("mariculture:void_bottle", 25, Rarity.JUNK, "end", Species.Rod.DIRE));
        l.add(new Loot("minecraft:ender_pearl", 15, Rarity.JUNK, "end", Species.Rod.OLD));
        l.add(new Loot("minecraft:ender_eye", 10, Rarity.JUNK, "end", Species.Rod.OLD));
        for (String disc : List.of("minecraft:music_disc_11", "minecraft:music_disc_mellohi", "minecraft:music_disc_stal")) {
            l.add(new Loot(disc, 5, Rarity.GOOD, "end", Species.Rod.GOOD));
        }
        l.add(new Loot("minecraft:experience_bottle", 35, Rarity.GOOD, null, null));
        l.add(new Loot("mariculture:iron_wheel", 25, Rarity.JUNK, null, null));
        LOOT = Collections.unmodifiableList(l);
    }

    /** Where and how the cast happens. {@code hat} is the worn bucket hat's colour effects, or null. */
    public record Cast(Species.Rod rod, int luckOfTheSea, String dimensionKind, int y, boolean day, Salinity salinity, int temperature,
                       FishingHat.Effects hat, double aliveModifierFromArmour, Where where) {
        /** A cast in plain water with no biome information (the net, the auto fisher, tests). */
        public Cast(Species.Rod rod, int luckOfTheSea, String dimensionKind, int y, boolean day, Salinity salinity, int temperature,
                    FishingHat.Effects hat, double aliveModifierFromArmour) {
            this(rod, luckOfTheSea, dimensionKind, y, day, salinity, temperature, hat, aliveModifierFromArmour, Where.WATER);
        }
    }

    /**
     * The biome and fluid at the hook, asked by id or {@code #tag} (species {@code catch.biomes} and {@code catch.fluid}).
     * {@code otherFluid} is true in a fluid that is neither water nor lava (another mod's, like Alex's Caves acid).
     */
    public interface Where {
        boolean inBiome(String idOrTag);

        boolean inFluid(String idOrTag);

        boolean otherFluid();

        Where WATER = new Where() {
            public boolean inBiome(String idOrTag) { return false; }
            public boolean inFluid(String idOrTag) { return false; }
            public boolean otherFluid() { return false; }
        };
    }

    /** Whether a species can bite at this hook's biome and fluid. */
    public static boolean placeMatches(Species s, Where where) {
        if (!s.catchBiomes().isEmpty() && s.catchBiomes().stream().noneMatch(where::inBiome)) return false;
        return s.catchFluid() != null ? where.inFluid(s.catchFluid()) : !where.otherFluid();
    }

    /** The catch: an item id (loot or a fish's raw form), or a live fish of a species with an optional forced gender. */
    public sealed interface Catch permits ItemCatch, LiveFish, VanillaLoot {}

    public record ItemCatch(String item, boolean enchant, int enchantLevel) implements Catch {}

    /** A live fish (the caller makes a pure fish of the species; {@code gender} is null for random). */
    public record LiveFish(String species, Integer gender, boolean enchant) implements Catch {}

    /**
     * Vanilla's own fishing loot: {@code table} is "junk", "treasure" or "fish" (legacy FishingHooks.getRandomFishable
     * fallbacks), optionally enchanted at {@code enchantLevel}.
     */
    public record VanillaLoot(String table, boolean enchant, int enchantLevel) implements Catch {}

    private FishingRules() {}

    /** FishingHandler.getCatch for a Mariculture rod (or vanilla's, as DIRE). */
    public static Catch cast(Cast c, FishRegistry registry, Rng rng) {
        RodStats rod = stats(c.rod());
        int lots = c.luckOfTheSea();
        Catch loot = null;
        if (lots > 0 && rng.nextInt(200) < lots * 10 || rng.nextInt(1000) < Math.max(0.01, rod.rare() * 10)) {
            loot = loot(c, Rarity.RARE, rng);
        } else if (lots > 0 && rng.nextInt(250) < lots * 10 || rng.nextInt(1000) < Math.max(0.01, rod.good() * 10)) {
            loot = loot(c, Rarity.GOOD, rng);
        } else if (lots > 0 && rng.nextInt(300) < lots * 10 || rng.nextInt(1000) < Math.max(0.01, rod.junk() * 10)) {
            loot = loot(c, Rarity.JUNK, rng);
        }
        boolean forceLoot = c.hat() != null && c.hat().forceLoot();
        boolean forceFish = c.hat() != null && c.hat().forceFish();
        if (!forceLoot && (loot == null || forceFish)) return fish(c, rod.quality(), registry, rng);
        if (loot == null) loot = new VanillaLoot(rng.nextInt(100) < rod.quality() ? "treasure" : "junk", false, 0);
        // Enchantable loot is enchanted quality% of the time at a random level below the rod's enchant value.
        boolean enchant = rng.nextInt(100) < rod.quality() && rod.enchant() > 0;
        int level = enchant ? rng.nextInt(rod.enchant()) : 0;
        boolean purple = c.hat() != null && c.hat().enchantLoot();
        if (loot instanceof ItemCatch i) return new ItemCatch(i.item(), enchant || purple, purple ? FishingHat.PURPLE_ENCHANT_LEVEL : level);
        VanillaLoot v = (VanillaLoot) loot;
        return new VanillaLoot(v.table(), enchant || purple, purple ? FishingHat.PURPLE_ENCHANT_LEVEL : level);
    }

    /** FishingHandler.getLoot: shuffled entries of the rarity, first that passes; else vanilla junk/treasure. */
    private static Catch loot(Cast c, Rarity rarity, Rng rng) {
        List<Loot> pool = pool(rarity, c.rod());
        shuffle(pool, rng);
        for (Loot l : pool) {
            if (l.dimension() != null && !l.dimension().equals(c.dimensionKind())) continue;
            if (l.minRod() != null && stats(c.rod()).quality() < stats(l.minRod()).quality()) continue;
            if (rng.nextInt(1000) < l.chancePercent() * 10) return new ItemCatch(l.item(), false, 0);
        }
        return new VanillaLoot(rarity == Rarity.JUNK ? "junk" : "treasure", false, 0);
    }

    /** The entries {@link #cast} draws from for a rarity on this rod, before the dimension and rod-quality checks. */
    private static List<Loot> pool(Rarity rarity, Species.Rod rod) {
        List<Loot> pool = new ArrayList<>();
        for (Loot l : LOOT) if (l.rarity() == rarity) pool.add(l);
        // Plugin loot registered with exact=true only comes on its own rod (Loot(stack, chance, rarity, dim, rod, true)).
        for (mariculture.domain.plugin.Plugins.PluginLoot p : mariculture.domain.plugin.Plugins.loot(rod)) {
            if (p.rarity() == rarity) pool.add(new Loot(p.item(), p.chancePercent(), rarity, null, null));
        }
        return mariculture.domain.plugin.Tweaks.loot(rarity, rod, pool);
    }

    /** Every loot entry this rod can land, rarest pool first (for recipe viewers). */
    public static List<Loot> lootFor(Species.Rod rod) {
        List<Loot> out = new ArrayList<>();
        for (Rarity r : new Rarity[] {Rarity.RARE, Rarity.GOOD, Rarity.JUNK}) {
            for (Loot l : pool(r, rod)) if (l.minRod() == null || stats(rod).quality() >= stats(l.minRod()).quality()) out.add(l);
        }
        return out;
    }

    /** FishingHandler.getFishForLocation (also the net, with rod quality 25). */
    public static Catch fish(Cast c, int rodQuality, FishRegistry registry, Rng rng) {
        Salinity salt = c.salinity();
        int temp = c.temperature();
        int y = c.y();
        String dim = c.dimensionKind();
        FishingHat.Effects hat = c.hat();
        if (hat != null) {
            if (hat.salinity() != null) salt = hat.salinity();
            if (hat.temperature() != null) temp = hat.temperature();
            if (hat.yHeight() != null) y = hat.yHeight();
            if (hat.nether()) dim = "nether";
        }
        double modifier = 10 + c.aliveModifierFromArmour();
        List<Species> catchable = new ArrayList<>(registry.allSpecies());
        catchable.sort((a, b) -> a.id().compareTo(b.id()));
        CatchChance.Context ctx = new CatchChance.Context(y, c.day(), dim);
        for (int i = 0; i < 30; i++) {
            shuffle(catchable, rng);
            for (Species s : catchable) {
                if (!dimensionMatches(s.catchDimension(), dim)) continue;
                if (!placeMatches(s, c.where())) continue;
                // A species of another fluid lives in that fluid, so water salinity and temperature do not apply to it.
                boolean ownFluid = s.catchFluid() != null;
                if (!ownFluid && !Environment.matches(salt, temp, s, s.salinityTolerance(), s.temperatureTolerance())) continue;
                double chance = s.catchChance().percent(ctx) * 10;
                if (chance > 0 && rodQuality >= stats(s.rod()).quality() && rng.nextInt(1000) < chance) {
                    // getCaughtAliveChance(world, salt, temp, y): exact salinity and accepted temperature only.
                    boolean env = ownFluid || temp >= s.temperatureBase() - s.temperatureTolerance() && temp <= s.temperatureBase() + s.temperatureTolerance()
                            && salt == s.salinityBase();
                    // catchFish: alive if nextInt(1000) < aliveChance% * (10 + hat bonuses) * 1.25.
                    double alive = env ? s.aliveChance().percent(ctx) * modifier : 0;
                    boolean isAlive = rng.nextInt(1000) < alive * ALIVE_MODIFIER;
                    // BloodRodType.caughtAlive: the bound rod always lands these species alive.
                    if (c.rod() == Species.Rod.BLOOD && mariculture.domain.plugin.Plugins.BLOOD_ALIVE.contains(s.id())) isAlive = true;
                    if (isAlive && hat != null && hat.alwaysDead()) isAlive = false;
                    if (!isAlive) return new ItemCatch(rawForm(s.id()), false, 0);
                    return new LiveFish(s.id(), hat == null ? null : hat.gender(), hat != null && hat.enchantLoot());
                }
            }
        }
        return new ItemCatch("minecraft:stick", false, 0);
    }

    /** core/config FishMechanics "Alive Modifier" default 1.25 (applied to chance * modifier, legacy catchFish). */
    public static final double ALIVE_MODIFIER = 1.25;

    /** FishSpecies.isWorldCorrect overrides by catch dimension kind. */
    public static boolean dimensionMatches(String speciesKind, String worldKind) {
        return switch (speciesKind) {
            case "nether" -> worldKind.equals("nether");
            case "end" -> worldKind.equals("end");
            case "not_nether" -> !worldKind.equals("nether");
            default -> worldKind.equals("overworld_like");
        };
    }

    public static String rawForm(String species) {
        mariculture.domain.fish.ShippedSpecies.Entry e = mariculture.domain.fish.ShippedSpecies.byId(species);
        return e != null ? e.rawItem() : "minecraft:cod";
    }

    /** BlockTicking.updateTick: a live fish when the net catches one, otherwise a raw cod. Null when nothing is caught. */
    public static mariculture.domain.machine.Stack net(String dimensionKind, int y, boolean day, Salinity salinity, int temperature,
                                                       FishRegistry registry, Rng rng) {
        // core/config Machines "Fishing Net" speed (default 300): a net catches on 1 in that many random ticks.
        if (rng.nextInt(mariculture.domain.config.Settings.NET_SPEED.get()) != 0) return null;
        Cast cast = new Cast(Species.Rod.DIRE, 0, dimensionKind, y, day, salinity, temperature, null, 0);
        Catch c = fish(cast, NET.quality(), registry, rng);
        if (c instanceof LiveFish) {
            mariculture.domain.machine.Stack s = toStack(c, registry, rng);
            if (s != null) return s;
        }
        return mariculture.domain.machine.Stack.of("minecraft:cod");
    }

    /** A live fish (pure, gender from the hat or random) or a plain item; null for vanilla loot, which the target rolls. */
    public static mariculture.domain.machine.Stack toStack(Catch c, FishRegistry registry, Rng rng) {
        if (c instanceof ItemCatch i) return mariculture.domain.machine.Stack.of(i.item());
        if (c instanceof LiveFish f) {
            Species s = registry.species(f.species());
            if (s == null) return mariculture.domain.machine.Stack.of(rawForm(f.species()));
            mariculture.domain.fish.Genome g = mariculture.domain.fish.Breeding.makePure(s, rng);
            mariculture.domain.machine.Stack stack = mariculture.domain.fish.LiveFish.stack(g);
            if (f.gender() != null) stack = mariculture.domain.fish.LiveFish.withGender(stack, f.gender());
            return stack;
        }
        return null;
    }

    private static <T> void shuffle(List<T> list, Rng rng) {
        for (int i = list.size() - 1; i > 0; i--) Collections.swap(list, i, rng.nextInt(i + 1));
    }
}

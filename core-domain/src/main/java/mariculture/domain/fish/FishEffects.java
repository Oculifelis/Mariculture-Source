package mariculture.domain.fish;

import java.util.ArrayList;
import java.util.List;

import mariculture.domain.random.Rng;

/**
 * Species effects, ported from the legacy FishSpecies subclasses (mariculture/fishery/fish/Fish*.java,
 * 1.3.0; lines in content-source/fish/species.json "effects"). Rules return actions; targets perform them.
 *
 * <p>Fixes (BUG-052): legacy built three potion effects with duration and amplifier swapped
 * (lungfish eat {@code (waterBreathing, 1, 2000)} and tank {@code (waterBreathing, 1, 500)}, jellyfish tank
 * {@code (poison, 1, 200)}), giving one-tick effects at absurd levels. They are applied as the intended
 * durations at level I. Clownfish's animal choice is kept as legacy behaved (the horse/chicken roll
 * overwrites the cow/sheep/pig rolls).
 */
public final class FishEffects {
    /** Effect ids are modern registry paths. Durations are ticks, amplifiers 0-based, as legacy PotionEffect. */
    public sealed interface Action permits Potion, Fire, EnderTeleport, Lightning, Give, Xp, Feed, Damage, Heal, Explode, WitherSkeleton {}

    public record Potion(String effect, int ticks, int amplifier, boolean ambient) implements Action {}

    public record Fire(int seconds) implements Action {}

    /** Random spot within +-range horizontally at the top solid or liquid block, not into lava (FishEnder.onConsumed). */
    public record EnderTeleport(int range) implements Action {}

    public record Lightning() implements Action {}

    public record Give(String item, int count) implements Action {}

    public record Xp(int points) implements Action {}

    public record Feed(int hunger, float saturation) implements Action {}

    /** {@code source}: "generic", "wither", "piranha" or "siamese" (legacy MaricultureDamage sources become generic mob damage). */
    public record Damage(String source, float amount) implements Action {}

    public record Heal(float amount) implements Action {}

    public record Explode(float power, boolean destroyBlocks) implements Action {}

    /** FishLamprey: turns a skeleton into a wither skeleton holding a stone sword with 4 attack damage. */
    public record WitherSkeleton() implements Action {}

    /** What a tank effect needs to know about one living entity in the tank water. */
    public record Living(boolean player, boolean skeleton, float health, int difficulty, boolean hostile) {
        public Living(boolean player, boolean skeleton, float health, int difficulty) {
            this(player, skeleton, health, difficulty, false);
        }
    }

    /** World effects of a working feeder (FishSpecies.affectWorld), resolved to concrete actions. */
    public sealed interface WorldAction permits SpawnMob, StoneToGlowstone, LightningNear, PushEnergy, BreedAnimals, CollectItems, SpawnLoot {}

    /** Spawn {@code entity} at tank coordinate {@code coord} if fewer than {@code cap} of {@code countType} are in the tank. */
    public record SpawnMob(String entity, String countType, int cap, int coord) implements WorldAction {}

    /** FishGlow: a stone block next to tank coordinate {@code coord} on side {@code face} becomes glowstone. */
    public record StoneToGlowstone(int coord, int face) implements WorldAction {}

    /** FishElectricRay: lightning at the feeder offset (dx, dz). */
    public record LightningNear(int dx, int dz) implements WorldAction {}

    /** FishElectricRay: offers RF/FE to every adjacent energy receiver. */
    public record PushEnergy(int amount) implements WorldAction {}

    /** FishDamsel: adult animals in the tank water fall in love, if fewer than {@code cap} animals are there. */
    public record BreedAnimals(int cap) implements WorldAction {}

    /** FishAngel: item entities within {@code range} of the feeder go into adjacent inventories. */
    public record CollectItems(int range) implements WorldAction {}

    /** FishKoi: one random item from a vanilla chest loot table appears at tank coordinate {@code coord}. */
    public record SpawnLoot(int coord) implements WorldAction {}

    /** Chest loot tables koi pick from (legacy ChestGenHooks categories mapped to vanilla tables). */
    public static final List<String> KOI_LOOT = List.of("minecraft:chests/simple_dungeon", "minecraft:chests/abandoned_mineshaft",
            "minecraft:chests/stronghold_corridor", "minecraft:chests/desert_pyramid", "minecraft:chests/jungle_temple");

    /** fishery/EntityBass.onImpact: 2 thrown damage to the entity hit, then a block-breaking 0.8 explosion. */
    public static final float BASS_BOMB_DAMAGE = 2f;
    public static final float BASS_BOMB_POWER = 0.8f;
    /** FishAngel.onRightClick: dead angelfish pull item entities within this many blocks to the player. */
    public static final int ANGELFISH_PULL_RANGE = 64;
    /** FishTrout: a raw rainbow trout hits for +10 attack damage and is used up. */
    public static final float TROUT_ATTACK_DAMAGE = 10f;

    /** What right-clicking a raw fish does instead of eating it (FishSpecies.onRightClick overrides). */
    public enum RightClick { EAT, THROW_BASS_BOMB, PULL_ITEMS }

    public static RightClick rightClick(String species) {
        return switch (path(species)) {
            case "bass" -> RightClick.THROW_BASS_BOMB;
            case "angelfish" -> RightClick.PULL_ITEMS;
            default -> RightClick.EAT;
        };
    }

    public static boolean isWeapon(String species) {
        return path(species).equals("rainbow_trout");
    }

    private FishEffects() {}

    private static String path(String species) {
        return species.startsWith("mariculture:") ? species.substring(12) : species;
    }

    /** onConsumed of the raw fish (ItemVanillaFishEdible.onEaten). */
    public static List<Action> eaten(String species, boolean day) {
        List<Action> a = new ArrayList<>();
        switch (path(species)) {
            case "pufferfish" -> {
                a.add(new Potion("poison", 1200, 3, false));
                a.add(new Potion("hunger", 300, 2, false));
                a.add(new Potion("nausea", 300, 1, false));
            }
            case "glowfish" -> a.add(new Give("minecraft:glowstone", 1));
            case "blazefish" -> a.add(new Fire(7));
            case "nightfish" -> { if (!day) a.add(new Potion("strength", 1800, 0, false)); }
            case "enderfish" -> a.add(new EnderTeleport(32));
            case "dragonfish" -> {
                a.add(new Feed(12, 0.5f));
                a.add(new Potion("night_vision", 2000, 0, false));
            }
            case "neon_tetra" -> a.add(new Potion("speed", 160, 0, false));
            case "piranha" -> a.add(new Potion("instant_damage", 20, 0, false));
            case "sting_ray" -> a.add(new Potion("nausea", 800, 1, false));
            case "manta_ray" -> a.add(new Potion("regeneration", 100, 1, false));
            case "electric_ray" -> a.add(new Lightning());
            case "damselfish" -> a.add(new Potion("regeneration", 100, 0, false));
            case "netherfish" -> a.add(new Potion("fire_resistance", 120, 0, false));
            case "jellyfish" -> {
                a.add(new Potion("instant_damage", 15, 0, false));
                a.add(new Potion("nausea", 200, 0, false));
            }
            case "man_o_war" -> {
                a.add(new Potion("instant_damage", 5, 1, false));
                a.add(new Potion("nausea", 1000, 1, false));
            }
            case "siamese_fighting_fish" -> { if (day) a.add(new Potion("strength", 600, 0, false)); }
            case "koi" -> a.add(new Potion("regeneration", 100, 2, false));
            case "blue_tang" -> a.add(new Potion("jump_boost", 80, 0, false));
            case "tuna" -> a.add(new Potion("speed", 800, 1, false));
            case "blaasop" -> {
                a.add(new Potion("instant_damage", 1, 0, false));
                a.add(new Potion("poison", 200, 5, false));
                a.add(new Potion("nausea", 500, 0, false));
            }
            case "spiderfish" -> a.add(new Potion("nausea", 300, 1, true));
            case "minecraft_fish" -> a.add(new Xp(8));
            case "chub" -> a.add(new Potion("hunger", 800, 1, false));
            case "lungfish" -> a.add(new Potion("water_breathing", 2000, 0, false)); // BUG-052: legacy (1 tick, level 2001)
            default -> { }
        }
        return a;
    }

    /** Whether the species has a tank living effect called by the feeder (FishSpecies.hasLivingEffect). */
    public static boolean hasLivingEffect(String species) {
        return switch (path(species)) {
            case "angelfish", "bass", "blaasop", "chub", "enderfish", "jellyfish", "lamprey", "lungfish", "man_o_war", "manta_ray",
                    "nightfish", "piranha", "siamese_fighting_fish", "spiderfish", "stargazer", "sting_ray", "tuna", "undead_fish" -> true;
            default -> false;
        };
    }

    /** FishSpecies.affectLiving for one entity in the tank water, every effect tick. */
    public static List<Action> living(String species, Living e, boolean day, Rng rng) {
        List<Action> a = new ArrayList<>();
        switch (path(species)) {
            case "angelfish" -> { if (e.health() < 1) a.add(new Heal(1)); }
            case "nightfish" -> { if (!day) a.add(new Potion("strength", 33, 2, true)); }
            case "bass" -> a.add(new Explode(1f, true));
            case "piranha" -> {
                if (e.player()) { if (e.difficulty() > 0) a.add(new Damage("piranha", e.difficulty())); }
                else a.add(new Damage("piranha", 2));
            }
            case "sting_ray" -> {
                if (e.player() && e.difficulty() > 0 && rng.nextInt(40 - e.difficulty() * 10) == 0) {
                    a.add(new Potion("poison", e.difficulty() * 100, 1, true));
                }
            }
            case "manta_ray" -> {
                a.add(new Potion("regeneration", 33, 0, true));
                a.add(new Heal(5));
            }
            case "jellyfish" -> a.add(new Potion("poison", 200, 0, false)); // BUG-052: legacy (1 tick, level 201)
            case "man_o_war" -> {
                a.add(new Potion("nausea", 250, 1, false));
                a.add(new Potion("blindness", 500, 0, false));
            }
            case "siamese_fighting_fish" -> { if (!e.player()) a.add(new Damage("siamese", 3)); }
            case "tuna" -> a.add(new Potion("speed", 33, 5, true));
            case "blaasop" -> {
                a.add(new Potion("nausea", 150, 1, true));
                a.add(new Potion("wither", 250, 0, true));
            }
            case "stargazer" -> { if (rng.nextInt(100) == 0) a.add(new Damage("wither", 1)); }
            case "lamprey" -> {
                if (e.skeleton()) { if (rng.nextInt(50) == 0) a.add(new WitherSkeleton()); }
                else if (rng.nextInt(20) == 0) a.add(new Damage("wither", 1));
            }
            case "spiderfish" -> { if (e.player()) a.add(new Potion("nausea", 150, 1, true)); }
            case "undead_fish" -> { if (e.player()) a.add(new Potion("hunger", 150, 1, true)); }
            case "chub" -> {
                if (e.player() && e.difficulty() > 0 && rng.nextInt(40 - e.difficulty() * 10) == 0) {
                    a.add(new Potion("absorption", e.difficulty() * 100, 1, true));
                    a.add(new Feed(1, 0.1f));
                }
            }
            case "lungfish" -> a.add(new Potion("water_breathing", 500, 0, false)); // BUG-052
            case "enderfish" -> { if (e.player() && rng.nextInt(10) == 0) a.addAll(eaten(species, day)); }
            default -> { }
        }
        return a;
    }

    /** FishSpecies.affectWorld for a working feeder whose tank has {@code coords} water blocks. */
    public static List<WorldAction> world(String species, Rng rng, int coords) {
        List<WorldAction> a = new ArrayList<>();
        switch (path(species)) {
            case "blazefish" -> spawn(a, rng, coords, 25, "minecraft:blaze", 5);
            case "perch" -> spawn(a, rng, coords, 10, "minecraft:bat", 3);
            case "catfish" -> spawn(a, rng, coords, 10, "minecraft:ocelot", 10);
            case "spiderfish" -> spawn(a, rng, coords, 10, "minecraft:spider", 10);
            case "undead_fish" -> spawn(a, rng, coords, 10, "minecraft:zombie", 10);
            case "boneless_fish" -> spawn(a, rng, coords, 10, "minecraft:skeleton", 10);
            case "pupfish" -> spawn(a, rng, coords, 10, "minecraft:wolf", 10);
            case "enderfish" -> spawn(a, rng, coords, 25, "minecraft:enderman", 5);
            case "clownfish" -> {
                if (rng.nextInt(10) == 0 && coords > 0) {
                    int coord = rng.nextInt(coords);
                    // Legacy draws the cow/sheep/pig rolls, then the horse roll replaces whatever they chose.
                    if (rng.nextInt(5) != 0 && rng.nextInt(5) != 0) rng.nextInt(5);
                    String animal = rng.nextInt(6) == 0 ? "minecraft:horse" : "minecraft:chicken";
                    a.add(new SpawnMob(animal, "animal", 10, coord));
                }
            }
            case "glowfish" -> { if (coords > 0) a.add(new StoneToGlowstone(rng.nextInt(coords), rng.nextInt(6))); }
            case "electric_ray" -> {
                if (rng.nextInt(512) == 0) a.add(new LightningNear(rng.nextInt(5) - rng.nextInt(10), rng.nextInt(5) - rng.nextInt(10)));
                a.add(new PushEnergy(200));
            }
            case "damselfish" -> { if (rng.nextInt(10) == 0) a.add(new BreedAnimals(10)); }
            case "angelfish" -> a.add(new CollectItems(16));
            case "koi" -> { if (rng.nextInt(25) == 0 && coords > 0) a.add(new SpawnLoot(rng.nextInt(coords))); }
            default -> { }
        }
        return a;
    }

    private static void spawn(List<WorldAction> a, Rng rng, int coords, int oneIn, String entity, int cap) {
        if (rng.nextInt(oneIn) == 0 && coords > 0) a.add(new SpawnMob(entity, entity, cap, rng.nextInt(coords)));
    }

    /** Ticks a dropped live fish item survives (ItemFishy.getEntityLifespan: FishSpecies.getOnLandLifespan 15, lungfish 1200). */
    public static int onLandLifespanTicks(String species) {
        return path(species).equals("lungfish") ? 1200 : 15;
    }
}

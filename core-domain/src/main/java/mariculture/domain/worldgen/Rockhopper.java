package mariculture.domain.worldgen;

import java.util.ArrayList;
import java.util.List;

import mariculture.domain.machine.Stack;
import mariculture.domain.random.Rng;

/**
 * The Rockhopper penguin (world/EntityRockhopper.java, 1.3.0; spawns WorldPlus.java:77-81): a tameable beach bird
 * that hunts squid and is tempted, bred and healed with fish.
 *
 * <p>Changes: legacy dropped a fishing catch when the Fishery module was on, turning a Mariculture fish into a vanilla
 * fish with the species id as its metadata (an invalid item past meta 3). Here the drops are the module-off branch,
 * random vanilla raw fish, which is what that code meant to give.
 */
public final class Rockhopper {
    public static final String ID = "rockhopper";
    /** setSize(0.3, 0.7). */
    public static final float WIDTH = 0.3f, HEIGHT = 0.7f;
    public static final double MAX_HEALTH = 15, MOVEMENT_SPEED = 0.15000001192092896;
    /** attackEntityAsMob: 10 damage. */
    public static final float ATTACK_DAMAGE = 10;
    /** Tamed rockhoppers heal 5 eating fish. */
    public static final float FISH_HEAL = 5;
    public static final float STEP_HEIGHT = 0.25f;
    /** Untamed rockhoppers despawn after 2400 ticks; at most 8 in a chunk. */
    public static final int DESPAWN_AFTER_TICKS = 2400, MAX_IN_CHUNK = 8;
    /** Beach and snowy beach, weight 10, groups of 3 to 11. */
    public static final int SPAWN_WEIGHT = 10, SPAWN_MIN = 3, SPAWN_MAX = 11;
    /** Hunts squid (EntityAINearestAttackableTarget chance 750). */
    public static final String PREY = "minecraft:squid";
    public static final String NEOPRENE = "mariculture:neoprene";
    /** Sound events (entity.rockhopper.ambient, ...) and their files (rockhopper_ambient.ogg, ...). */
    public static final List<String> SOUNDS = List.of("ambient", "hurt", "death");
    /** Biomes it spawns in (BiomeGenBase.beach and coldBeach). */
    public static final List<String> BIOMES = List.of("minecraft:beach", "minecraft:snowy_beach");
    public static final List<String> FISH = List.of("minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish", "minecraft:pufferfish");

    private Rockhopper() {}

    /** attackEntityFrom: damage from something that is neither a player nor an arrow is halved, (damage + 1) / 2. */
    public static float incomingDamage(float damage, boolean fromEntity, boolean fromPlayerOrArrow) {
        return fromEntity && !fromPlayerOrArrow ? (damage + 1) / 2 : damage;
    }

    /**
     * dropFewItems: {@code j = nextInt(3) + nextInt(1 + looting)} raw fish of one random kind; on a player kill with
     * j above 2 a neoprene, with j of 2 one more raw fish.
     */
    public static List<Stack> drops(boolean playerKill, int looting, Rng rng) {
        List<Stack> out = new ArrayList<>();
        int j = rng.nextInt(3) + rng.nextInt(1 + Math.max(0, looting));
        if (j > 0) out.add(Stack.of(FISH.get(rng.nextInt(4)), j));
        if (playerKill && j > 1) {
            out.add(j > 2 ? Stack.of(NEOPRENE) : Stack.of(FISH.get(rng.nextInt(4)), j));
        }
        return out;
    }
}

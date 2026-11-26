package mariculture.domain.fishing;

import java.util.Optional;

import mariculture.domain.config.Probability;
import mariculture.domain.pearl.PearlColor;
import mariculture.domain.random.Rng;

/**
 * Zombies that spawn wearing a fishing hat and holding a Mariculture rod.
 *
 * <p>Legacy: mariculture.fishery.FisheryEventHandler#onEntityJoinWorld (1.3.0, lines 104-124).
 * Two defects are fixed here (BUG-001):
 * <ul>
 *   <li>The roll was {@code nextFloat() - 0.1F < chance}, which adds 0.1 to every configured
 *   chance (0.0125 behaved like 0.1125). The configured chance is now used as-is.</li>
 *   <li>The roll ran on every EntityJoinWorld, so an unequipped zombie re-rolled each time its
 *   chunk reloaded. Adapters must only call {@link #roll} for a zombie's first natural
 *   spawn (finalize-spawn), never on load.</li>
 * </ul>
 * Rod tier odds, hat colour, effects and drop chances are unchanged.
 */
public final class ZombieFishingGear {
    /** Effect durations/amplifiers from the legacy handler (ticks, 0-based amplifier). */
    public static final int SPEED_TICKS = 6000;
    public static final int SPEED_AMPLIFIER = 1;
    public static final int RESISTANCE_TICKS = 18000;
    public static final int RESISTANCE_AMPLIFIER = 2;
    public static final int STRENGTH_TICKS = 18000;
    public static final int STRENGTH_AMPLIFIER = 2;

    /** Legacy config defaults (GeneralStuff, 1.3.0 lines 40-42). */
    public static final double DEFAULT_SPAWN_CHANCE = 0.0125;
    public static final double DEFAULT_HAT_DROP_CHANCE = 0.1;
    public static final double DEFAULT_ROD_DROP_CHANCE = 0.05;

    public enum Rod {
        REED("reed_fishing_rod"), WOOD("wooden_fishing_rod"), TITANIUM("titanium_fishing_rod");

        private final String itemId;

        Rod(String itemId) {
            this.itemId = itemId;
        }

        /** Catalog item path of this rod. */
        public String itemId() {
            return itemId;
        }
    }

    public record Loadout(Rod rod, PearlColor hatColor) {
        /** Catalog item path of the dyed bucket hat (legacy ItemArmorFishingHat.getDyed). */
        public String hatItemId() {
            return FishingHat.itemId(hatColor);
        }
    }

    private ZombieFishingGear() {}

    /**
     * Decides a freshly spawned zombie's gear. Returns empty when the chance fails or the
     * zombie already holds something or wears a helmet, in which case nothing changes.
     * Draw order matches the legacy code so seeded behaviour is comparable.
     */
    public static Optional<Loadout> roll(Probability chance, boolean mainHandEmpty, boolean headEmpty, Rng rng) {
        if (chance.isNever() || !chance.test(rng.nextFloat())) {
            return Optional.empty();
        }
        if (!mainHandEmpty || !headEmpty) {
            return Optional.empty();
        }
        Rod rod;
        if (rng.nextInt(100) == 0) {
            rod = Rod.TITANIUM;
        } else if (rng.nextInt(33) == 0) {
            rod = Rod.WOOD;
        } else {
            rod = Rod.REED;
        }
        return Optional.of(new Loadout(rod, PearlColor.byLegacyMeta(rng.nextInt(PearlColor.count()))));
    }
}

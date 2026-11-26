package mariculture.domain.magic;

import java.util.List;

/**
 * The elemental enchantment (magic/enchantments/EnchantmentElemental.java, magic/jewelry/parts/MaterialPearl*.java,
 * 1.3.0): while any worn piece has it, the pearl material of every worn piece adds its effect. Each effect may wear
 * down the pieces with elemental made of that material, one point with a 1-in-{@code wearOneIn} chance (0: never).
 *
 * <p>Legacy cancelled some damage in the attack event and checked the same damage again in the hurt event, which the
 * cancel had already stopped; only what could happen is kept (blue, red and yellow make their damage harmless and
 * wear nothing). Brown (BUG-060) wore its piece on every block broken and dropped the bait's template stack instead
 * of the rolled one; here only dirt and grass count and the rolled count drops.
 */
public final class Elemental {
    /** Incoming damage the materials care about (legacy DamageSource constants). */
    public enum Damage { DROWN, MAGIC, FIRE, CACTUS, FALLING_BLOCK, IN_WALL, OTHER }

    /** What happens to damage the wearer takes. */
    public record Defence(boolean cancel, boolean extinguish, boolean curePoison, int wearOneIn) {
        static final Defence NONE = new Defence(false, false, false, 0);
    }

    /** What happens when the wearer hits something: extra magic damage factor, healing, wear. */
    public record Attack(float extraMagicFactor, float heal, int wearOneIn) {
        static final Attack NONE = new Attack(0, 0, 0);
    }

    /** A kill: gold nugget, experience orb (value), a fishing catch (silverfish); wear. */
    public record Kill(boolean goldNugget, int xpOrb, boolean fishingCatch, int wearOneIn) {
        static final Kill NONE = new Kill(false, -1, false, 0);
    }

    private Elemental() {}

    /** MaterialPearl*.onAttacked / onHurt. */
    public static Defence defend(JewelryMaterial m, Damage d, boolean poisoned) {
        return switch (m) {
            case BLUE -> d == Damage.DROWN ? new Defence(true, false, false, 0) : Defence.NONE;
            case GREEN -> d != Damage.MAGIC ? Defence.NONE
                    : poisoned ? new Defence(true, false, true, 0) : new Defence(false, false, false, 3);
            case PURPLE -> d != Damage.MAGIC ? Defence.NONE
                    : poisoned ? new Defence(false, false, false, 80) : new Defence(true, false, false, 0);
            case RED -> d == Damage.FIRE ? new Defence(true, true, false, 0) : Defence.NONE;
            case YELLOW -> d == Damage.CACTUS || d == Damage.FALLING_BLOCK || d == Damage.IN_WALL ? new Defence(true, false, false, 0) : Defence.NONE;
            default -> Defence.NONE;
        };
    }

    /** MaterialPearl*.onAttack: black at night and white by day add 1.25x as magic damage; pink heals a fifth. */
    public static Attack attack(JewelryMaterial m, boolean day, boolean canHeal, float amount) {
        return switch (m) {
            case BLACK -> day ? Attack.NONE : new Attack(1.25f, 0, 5);
            case WHITE -> day ? new Attack(1.25f, 0, 5) : Attack.NONE;
            case PINK -> canHeal ? new Attack(0, amount / 5, 3) : Attack.NONE;
            default -> Attack.NONE;
        };
    }

    /** MaterialPearl*.onKill. {@code silverfish}: the victim was a silverfish. */
    public static Kill kill(JewelryMaterial m, boolean silverfish, mariculture.domain.random.Rng rng) {
        return switch (m) {
            case GOLD -> rng.nextInt(50) == 0 ? new Kill(true, -1, false, 2) : Kill.NONE;
            case ORANGE -> new Kill(false, rng.nextInt(10), false, 10);
            case SILVER -> silverfish && rng.nextInt(8) == 0 ? new Kill(false, -1, true, 3) : Kill.NONE;
            default -> Kill.NONE;
        };
    }

    /**
     * MaterialPearlBrown.onBlockBreak: breaking dirt or grass rolls the sifter's dirt table; the piece wears 1 in
     * (64 - drops). Returns the wear chance, 0 for other blocks.
     */
    public static int breakWear(JewelryMaterial m, boolean dirtOrGrass, int drops) {
        if (m != JewelryMaterial.BROWN || !dirtOrGrass) return 0;
        return Math.max(1, 64 - drops);
    }

    /** Whether a wear roll hits. */
    public static boolean wears(int oneIn, mariculture.domain.random.Rng rng) {
        return oneIn > 0 && rng.nextInt(oneIn) == 0;
    }

    /** The materials that act: those of worn pieces, when any worn piece has elemental. */
    public static List<JewelryMaterial> active(List<WornJewelry.Piece> worn) {
        if (WornJewelry.strength(worn, JewelryEnchantment.ELEMENTAL.id()) <= 0) return List.of();
        List<JewelryMaterial> out = new java.util.ArrayList<>();
        for (WornJewelry.Piece p : worn) if (p.material() != null) out.add(p.material());
        return out;
    }
}

package mariculture.domain.magic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The jewelry a player wears (magic/MirrorHandler.java, MirrorHelper.java, core/helpers/EnchantHelper.java, 1.3.0).
 * The mirror's first three slots are the ring, bracelet and necklace slot; the fourth is only for enchanting and
 * is emptied when the mirror closes.
 *
 * <p>Changes: legacy kept the slots in world save data per player name; here jewelry is worn in the Curios ring,
 * bracelet and necklace slots (all of them count, so extra slots from other mods add up) and the mirror shows the
 * first of each. A {@link Piece}'s slot is its index in that list on the loader side. Legacy
 * counted the enchanting slot's pearl material for elemental effects; here only worn pieces count.
 */
public final class WornJewelry {
    public static final int SLOTS = 4, ENCHANT_SLOT = 3;

    /** One worn piece: its slot, material, enchantments and whether it is broken (damage at its maximum). */
    public record Piece(int slot, JewelryMaterial material, Map<String, Integer> enchantments, boolean broken, JewelryType type) {
        /** A piece whose jewelry type is not known: it always adds to the strength. */
        public Piece(int slot, JewelryMaterial material, Map<String, Integer> enchantments, boolean broken) {
            this(slot, material, enchantments, broken, null);
        }
    }

    private WornJewelry() {}

    /**
     * MirrorHandler.getEnchantmentStrength: the levels of an enchantment on all unbroken pieces, added up. The legacy
     * mod had one ring, one bracelet and one necklace; with extra Curios slots only the strongest piece of each
     * jewelry type counts, unless the {@code jewelry.extra_slots_stack} setting is on.
     */
    public static int strength(List<Piece> worn, String enchantment) {
        return strength(worn, enchantment, mariculture.domain.config.Settings.JEWELRY_EXTRA_SLOTS_STACK.get());
    }

    public static int strength(List<Piece> worn, String enchantment, boolean extraSlotsStack) {
        int total = 0;
        java.util.Map<JewelryType, Integer> best = new java.util.EnumMap<>(JewelryType.class);
        for (Piece p : worn) {
            if (p.broken()) continue;
            int level = p.enchantments().getOrDefault(enchantment, 0);
            if (extraSlotsStack || p.type() == null) total += level;
            else best.merge(p.type(), level, Math::max);
        }
        for (int level : best.values()) total += level;
        return total;
    }

    /**
     * MirrorHandler.handleDamage: the slots that take one point of wear. Unbroken pieces with the enchantment wear
     * (EnchantHelper.getLevel reads 0 on a broken piece); for elemental only pieces of the given material. Creative
     * players never wear jewelry (EnchantHelper.damageItems); the target checks that.
     */
    public static List<Integer> wearSlots(List<Piece> worn, String enchantment, JewelryMaterial elementalMaterial) {
        List<Integer> out = new ArrayList<>();
        for (Piece p : worn) {
            if (p.broken() || p.enchantments().getOrDefault(enchantment, 0) <= 0) continue;
            if (elementalMaterial != null && p.material() != elementalMaterial) continue;
            out.add(p.slot());
        }
        return out;
    }

    /** Which mirror slot accepts an item: the type's own slot for jewelry, the enchanting slot for anything else. */
    public static boolean accepts(int slot, String item) {
        JewelryType t = JewelryType.byItem(item);
        if (slot == ENCHANT_SLOT) return true;
        return t != null && t.slot == slot;
    }
}

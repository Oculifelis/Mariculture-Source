package mariculture.platform.magic;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import mariculture.domain.content.Ids;
import mariculture.domain.magic.JewelryEnchantment;

/**
 * The jewelry enchantments in code (1.20.1 has no enchantment data): pearls only, never on books, at the table, in
 * trades or in loot, and none goes with another jewelry enchantment. The One Ring is treasure. Rarity is the
 * nearest vanilla weight; the mirror uses the exact weights from JewelryEnchantment.
 */
public final class ModEnchantments {
    private static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, Ids.MOD_ID);
    static final EnchantmentCategory PEARLS = EnchantmentCategory.create("MARICULTURE_PEARLS", item -> {
        var key = ForgeRegistries.ITEMS.getKey(item);
        return key != null && key.getNamespace().equals(Ids.MOD_ID) && key.getPath().endsWith("_pearl");
    });

    private ModEnchantments() {}

    public static void register(IEventBus modBus) {
        for (JewelryEnchantment e : JewelryEnchantment.values()) ENCHANTMENTS.register(e.path(), () -> new Jewel(e));
        ENCHANTMENTS.register(modBus);
    }

    private static Enchantment.Rarity rarity(int weight) {
        return weight >= 10 ? Enchantment.Rarity.COMMON : weight >= 5 ? Enchantment.Rarity.UNCOMMON
                : weight >= 2 ? Enchantment.Rarity.RARE : Enchantment.Rarity.VERY_RARE;
    }

    static final class Jewel extends Enchantment {
        private final JewelryEnchantment rule;

        Jewel(JewelryEnchantment rule) {
            super(rarity(rule.weight), PEARLS, EquipmentSlot.values());
            this.rule = rule;
        }

        @Override
        public int getMaxLevel() {
            return rule.maxLevel;
        }

        @Override
        public int getMinCost(int level) {
            return rule.minCost(level);
        }

        @Override
        public int getMaxCost(int level) {
            return rule.maxCost(level);
        }

        @Override
        protected boolean checkCompatibility(Enchantment other) {
            return !(other instanceof Jewel);
        }

        @Override
        public boolean isTreasureOnly() {
            return rule == JewelryEnchantment.ONE_RING;
        }

        @Override
        public boolean isTradeable() {
            return false;
        }

        @Override
        public boolean isDiscoverable() {
            return false;
        }

        @Override
        public boolean isAllowedOnBooks() {
            return false;
        }
    }
}

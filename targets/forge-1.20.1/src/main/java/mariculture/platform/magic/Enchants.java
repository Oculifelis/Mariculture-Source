package mariculture.platform.magic;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

/** Enchantments on a loader stack as registry id to level, for the jewelry rules. */
public final class Enchants {
    private Enchants() {}

    public static Map<String, Integer> read(ItemStack stack) {
        Map<String, Integer> out = new LinkedHashMap<>();
        EnchantmentHelper.getEnchantments(stack).forEach((e, level) -> out.put(String.valueOf(ForgeRegistries.ENCHANTMENTS.getKey(e)), level));
        return out;
    }

    /** Replaces the stack's enchantments; ids the registry does not know are skipped. */
    public static void apply(ItemStack stack, Map<String, Integer> enchantments, HolderLookup.Provider access) {
        Map<Enchantment, Integer> map = new LinkedHashMap<>();
        enchantments.forEach((id, level) -> {
            Enchantment e = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(id));
            if (e != null && ForgeRegistries.ENCHANTMENTS.containsKey(new ResourceLocation(id))) map.put(e, level);
        });
        EnchantmentHelper.setEnchantments(map, stack);
    }
}

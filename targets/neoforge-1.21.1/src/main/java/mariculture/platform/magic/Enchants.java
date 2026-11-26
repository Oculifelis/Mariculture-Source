package mariculture.platform.magic;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Enchantments on a loader stack as registry id to level, for the jewelry rules. */
public final class Enchants {
    private Enchants() {}

    public static Map<String, Integer> read(ItemStack stack) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (var e : stack.getEnchantments().entrySet()) out.put(e.getKey().getRegisteredName(), e.getIntValue());
        return out;
    }

    /** An enchantment by registry id. */
    public static java.util.Optional<? extends net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>> holder(HolderLookup.Provider access, String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) return java.util.Optional.empty();
        return access.lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT, key));
    }

    /** Replaces the stack's enchantments; ids the registry does not know are skipped. */
    public static void apply(ItemStack stack, Map<String, Integer> enchantments, HolderLookup.Provider access) {
        ItemEnchantments.Mutable m = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.forEach((id, level) -> holder(access, id).ifPresent(h -> m.set(h, level)));
        EnchantmentHelper.setEnchantments(stack, m.toImmutable());
    }
}

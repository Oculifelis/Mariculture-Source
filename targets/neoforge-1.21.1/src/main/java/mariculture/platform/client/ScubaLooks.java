package mariculture.platform.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import mariculture.domain.content.Ids;
import mariculture.domain.diving.Scuba;
import mariculture.platform.Stacks;

/**
 * Thaumaturge looks for scuba gear: item model overrides on the warded flag and the revealing upgrade. Without
 * Thaumaturge no stack carries either mark, so nothing changes. Worn layers are picked in ScubaArmorItem.
 */
public final class ScubaLooks {
    private ScubaLooks() {}

    private static final ResourceLocation GOGGLES = ResourceLocation.fromNamespaceAndPath("thaumaturge", "goggles_upgrade");

    static boolean warded(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(Scuba.WARDED);
    }

    static boolean revealing(ItemStack stack) {
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(GOGGLES);
        return type != null && stack.has(type);
    }

    static void properties() {
        for (String id : new String[] {Scuba.MASK, Scuba.SUIT, Scuba.FINS}) {
            Item item = Stacks.item(Ids.MOD_ID + ":" + id);
            ItemProperties.register(item, ResourceLocation.fromNamespaceAndPath(Ids.MOD_ID, "warded"), (stack, level, entity, seed) -> warded(stack) ? 1f : 0f);
            if (id.equals(Scuba.MASK)) {
                ItemProperties.register(item, ResourceLocation.fromNamespaceAndPath(Ids.MOD_ID, "revealing"), (stack, level, entity, seed) -> revealing(stack) ? 1f : 0f);
            }
        }
    }
}

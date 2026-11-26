package mariculture.platform.compat.forestry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import forestry.api.IForestryApi;
import forestry.api.core.backpacks.EnumBackpackType;
import forestry.api.core.backpacks.IBackpackDefinition;
import forestry.core.platform.item.ITintedItem;
import mariculture.domain.plugin.Plugins;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The part of the Forestry plugin (PluginForestry, PLUG-004) that needs Forestry 3.0's Java API: the aquatic backpacks
 * (Plugins.AQUA_BACKPACK) made through its backpack interface. Its fuels are data maps here (ResourceExporter). Only
 * touched while Forestry is loaded.
 */
public final class ForestryCompat {
    private static final TagKey<Item> ALLOWED = ItemTags.create(ResourceLocation.parse(Plugins.AQUA_BACKPACK_TAG));
    private static final List<DeferredItem<Item>> BACKPACKS = new ArrayList<>();
    private static final IBackpackDefinition AQUA = new IBackpackDefinition() {
        @Override
        public Component getName(ItemStack backpack) {
            return Component.translatable(backpack.getItem().getDescriptionId());
        }

        @Override
        public int getPrimaryColour() {
            return Plugins.AQUA_PRIMARY;
        }

        @Override
        public int getSecondaryColour() {
            return Plugins.AQUA_SECONDARY;
        }

        @Override
        public Predicate<ItemStack> getFilter() {
            return stack -> stack.is(ALLOWED);
        }
    };

    private ForestryCompat() {}

    public static void registerBackpacks(DeferredRegister.Items items, List<Supplier<? extends Item>> tab) {
        BACKPACKS.add(items.register(Plugins.AQUA_BACKPACK,
                () -> IForestryApi.INSTANCE.getBackpackInterface().createBackpack(AQUA, EnumBackpackType.NORMAL)));
        BACKPACKS.add(items.register(Plugins.WOVEN_AQUA_BACKPACK,
                () -> IForestryApi.INSTANCE.getBackpackInterface().createBackpack(AQUA, EnumBackpackType.WOVEN)));
        tab.addAll(BACKPACKS);
    }

    /** Forestry tints its backpack sprites with the definition's colours. */
    public static void colors(RegisterColorHandlersEvent.Item event) {
        for (DeferredItem<Item> b : BACKPACKS) {
            event.register((stack, tint) -> ((ITintedItem) stack.getItem()).getColorFromItemStack(stack, tint), b.get());
        }
    }
}

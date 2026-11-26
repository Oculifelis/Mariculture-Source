package mariculture.platform.compat.forestry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import forestry.api.fuels.EngineBronzeFuel;
import forestry.api.fuels.FermenterFuel;
import forestry.api.fuels.FuelManager;
import forestry.api.storage.EnumBackpackType;
import forestry.api.storage.IBackpackDefinition;
import forestry.core.items.definitions.IColoredItem;
import forestry.storage.ModuleStorage;
import mariculture.domain.fluid.Fluids;
import mariculture.domain.plugin.Plugins;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The parts of the Forestry plugin (PluginForestry, PLUG-004) that need Forestry 2.x's Java API: the aquatic backpacks
 * (Plugins.AQUA_BACKPACK) made through its backpack interface, and fish oil and kelp in its fuel maps. Only touched
 * while Forestry is loaded.
 */
public final class ForestryCompat {
    private static final TagKey<Item> ALLOWED = ItemTags.create(new ResourceLocation(Plugins.AQUA_BACKPACK_TAG));
    private static final List<RegistryObject<Item>> BACKPACKS = new ArrayList<>();
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

    public static void registerBackpacks(DeferredRegister<Item> items, List<RegistryObject<? extends Item>> tab) {
        BACKPACKS.add(items.register(Plugins.AQUA_BACKPACK, () -> ModuleStorage.BACKPACK_INTERFACE.createBackpack(AQUA, EnumBackpackType.NORMAL)));
        BACKPACKS.add(items.register(Plugins.WOVEN_AQUA_BACKPACK, () -> ModuleStorage.BACKPACK_INTERFACE.createBackpack(AQUA, EnumBackpackType.WOVEN)));
        tab.addAll(BACKPACKS);
    }

    /** Forestry tints its backpack sprites with the definition's colours. */
    public static void colors(RegisterColorHandlersEvent.Item event) {
        for (RegistryObject<Item> b : BACKPACKS) {
            event.register((stack, tint) -> ((IColoredItem) stack.getItem()).getColorFromItemStack(stack, tint), b.get());
        }
    }

    /** Fish oil in the biogas engine and kelp in the fermenter (PluginForestry.java:97, 113); at common setup. */
    public static void fuels() {
        if (!Plugins.active(Plugins.FORESTRY) || FuelManager.fermenterFuel == null || FuelManager.biogasEngineFuel == null) return;
        ItemStack kelp = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(Plugins.FERMENTER_KELP)));
        FuelManager.fermenterFuel.put(kelp, new FermenterFuel(kelp, Plugins.KELP_FERMENT_PER_CYCLE, Plugins.KELP_BURN_DURATION));
        Fluid oil = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(Fluids.FISH_OIL));
        if (oil != null) {
            FuelManager.biogasEngineFuel.put(oil, new EngineBronzeFuel(oil, Plugins.FISH_OIL_BIOGAS_POWER, Plugins.FISH_OIL_BIOGAS_BURN,
                    Plugins.FISH_OIL_BIOGAS_DISSIPATION));
        }
    }
}

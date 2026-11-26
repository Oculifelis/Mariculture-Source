package mariculture.platform.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;

import mariculture.domain.fishery.Droplets;
import mariculture.domain.machine.Stack;
import mariculture.platform.Stacks;

/** One droplet for any metal fluid (Droplets.METAL): named after the fluid in its item data and tinted its colour. */
public class MetalDropletItem extends Item {
    public MetalDropletItem(Properties props) {
        super(props);
    }

    @Override
    public Component getName(ItemStack stack) {
        Droplets.Droplet d = Droplets.of(Stacks.toDomain(stack));
        Identifier key = d == null ? null : Identifier.tryParse(d.fluid());
        if (key == null || !BuiltInRegistries.FLUID.containsKey(key)) return super.getName(stack);
        Fluid f = BuiltInRegistries.FLUID.getValue(key);
        if (f == Fluids.EMPTY) return super.getName(stack);
        return Component.translatable("item.mariculture.metal_droplet.named", f.getFluidType().getDescription());
    }

    /** Puts the droplet's tint in its custom model data, read by the item model's custom_model_data tint (Stacks.toLoader). */
    public static void apply(ItemStack out, Stack s) {
        if (!(out.getItem() instanceof MetalDropletItem) || Droplets.of(s) == null) return;
        out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(java.util.List.of(), java.util.List.of(), java.util.List.of(),
                java.util.List.of(Droplets.color(s))));
    }
}

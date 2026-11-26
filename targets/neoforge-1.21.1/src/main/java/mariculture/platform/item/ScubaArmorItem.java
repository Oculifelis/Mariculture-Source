package mariculture.platform.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import mariculture.domain.content.Ids;
import mariculture.domain.diving.Scuba;
import mariculture.domain.machine.Stack;
import mariculture.platform.Stacks;

/** Scuba gear (domain Scuba): the tank shows its air as the bar; sneak-using the mask switches its land lights. */
public class ScubaArmorItem extends ArmorItem {
    public ScubaArmorItem(net.minecraft.core.Holder<ArmorMaterial> material, Type type, Properties props) {
        super(material, type, props);
    }

    private static boolean is(ItemStack stack, String id) {
        return Stacks.id(stack.getItem()).equals(Ids.MOD_ID + ":" + id);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (is(stack, Scuba.MASK) && player.isShiftKeyDown()) {
            if (!level.isClientSide) Stacks.setData(stack, Scuba.toggleLandLights(Stacks.toDomain(stack)).data());
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return is(stack, Scuba.TANK) || super.isBarVisible(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return is(stack, Scuba.TANK) ? Scuba.bar(Stacks.toDomain(stack)) : super.getBarWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return is(stack, Scuba.TANK) ? 0x55AAFF : super.getBarColor(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) {
        Stack s = Stacks.toDomain(stack);
        if (is(stack, Scuba.TANK)) {
            lines.add(Component.translatable("item.mariculture.scuba_tank.air", Scuba.air(s), Scuba.TANK_AIR).withStyle(ChatFormatting.AQUA));
        } else if (is(stack, Scuba.MASK)) {
            Component state = Scuba.landLights(s) ? Component.translatable("item.mariculture.scuba_mask.on").withStyle(ChatFormatting.DARK_GREEN)
                    : Component.translatable("item.mariculture.scuba_mask.off").withStyle(ChatFormatting.RED);
            lines.add(Component.translatable("item.mariculture.scuba_mask.land_lights", state).withStyle(ChatFormatting.GRAY));
        }
    }

    /** Warded pieces (Thaumaturge infusion) wear the warded armour layers; others keep their material's. */
    @Override
    public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack, net.minecraft.world.entity.Entity entity,
                                                                    net.minecraft.world.entity.EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(Scuba.WARDED)) return super.getArmorTexture(stack, entity, slot, layer, innerModel);
        String set = is(stack, Scuba.FINS) ? "warded_swimfin" : "warded_scuba";
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Ids.MOD_ID,
                "textures/models/armor/" + set + "_layer_" + (innerModel ? 2 : 1) + ".png");
    }
}

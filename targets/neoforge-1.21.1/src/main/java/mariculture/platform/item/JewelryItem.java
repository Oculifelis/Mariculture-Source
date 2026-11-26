package mariculture.platform.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import mariculture.domain.machine.Stack;
import mariculture.domain.magic.Binding;
import mariculture.domain.magic.Jewelry;
import mariculture.domain.magic.JewelryMaterial;
import mariculture.domain.magic.JewelryType;
import mariculture.platform.Stacks;
import mariculture.platform.magic.JewelryEvents;
import net.minecraft.world.damagesource.DamageSource;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;

/**
 * A ring, bracelet or necklace (legacy ItemJewelry): named after its pearl, with its parts and limits in the tooltip.
 * Durability and the model come from its parts, set on the stack by {@link #apply}. Worn in the Curios ring,
 * bracelet and necklace slots (the item tags curios:ring, curios:bracelet and curios:necklace).
 */
public class JewelryItem extends Item implements ICurioItem {
    private final JewelryType type;

    public JewelryItem(Properties props, JewelryType type) {
        super(props);
        this.type = type;
    }

    /** Puts the parts' durability and model on a stack made from domain data (Stacks.toLoader). */
    public static void apply(ItemStack out, Stack s) {
        if (!(out.getItem() instanceof JewelryItem)) return;
        int durability = Jewelry.durability(s);
        if (durability > 0) {
            out.set(DataComponents.MAX_DAMAGE, durability);
            if (!out.has(DataComponents.DAMAGE)) out.set(DataComponents.DAMAGE, 0);
        }
        if (Jewelry.modelIndex(s) > 0) out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(Jewelry.modelIndex(s)));
    }

    @Override
    public Component getName(ItemStack stack) {
        Stack s = Stacks.toDomain(stack);
        if (Jewelry.isOneRing(s)) return Component.translatable("item.mariculture.jewelry.one_ring").withStyle(ChatFormatting.GOLD);
        JewelryMaterial m = Jewelry.material(s);
        if (m == null || Stacks.item(m.item()) == null) return super.getName(stack);
        return Component.translatable("item.mariculture.jewelry.named", new ItemStack(Stacks.item(m.item())).getHoverName(), super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
        java.util.function.Consumer<Component> lines = list::add;
        Stack s = Stacks.toDomain(stack);
        if (Jewelry.isOneRing(s)) {
            lines.accept(Component.translatable("item.mariculture.jewelry.one_ring.lore").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }
        Binding b = Jewelry.binding(s);
        JewelryMaterial m = Jewelry.material(s);
        if (b == null || m == null) return;
        lines.accept(Component.translatable("item.mariculture.jewelry.with", new ItemStack(Stacks.item(b.item(type))).getHoverName()).withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.mariculture.jewelry.elemental",
                Component.translatable("item.mariculture.jewelry.elemental." + m.id())).withStyle(ChatFormatting.DARK_AQUA));
        lines.accept(Component.translatable("item.mariculture.jewelry.max_enchants", Jewelry.maxEnchantments(type, m)).withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.mariculture.jewelry.max_level", Jewelry.maxLevel(type, b, m)).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean canEquipFromUse(SlotContext ctx, ItemStack stack) {
        return true;
    }

    @Override
    public DropRule getDropRule(SlotContext ctx, DamageSource source, boolean recentlyHit, ItemStack stack) {
        return JewelryEvents.keepsJewelry(ctx.entity(), source) ? DropRule.ALWAYS_KEEP : DropRule.DEFAULT;
    }
}

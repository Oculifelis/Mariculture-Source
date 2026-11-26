package mariculture.platform.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import mariculture.domain.magic.MirrorEnchanting;
import mariculture.platform.Stacks;

/**
 * A mirror (legacy ItemMirror, ItemMagicMirror): use opens the jewelry mirror and wears it one point (not in
 * creative); sneak-use on a magic or celestial mirror moves on to its next three enchanting levels.
 */
public class MirrorItem extends Item {
    private final net.minecraft.tags.TagKey<Item> repair;

    public MirrorItem(Properties props, String id) {
        super(props);
        this.repair = net.minecraft.tags.ItemTags.create(ResourceLocation.fromNamespaceAndPath(mariculture.domain.content.Ids.MOD_ID, "repairs/" + id));
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack material) {
        return material.is(repair);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String id = Stacks.id(this);
        if (player.isShiftKeyDown()) {
            if (MirrorEnchanting.range(id) == null) return InteractionResultHolder.pass(stack);
            if (!level.isClientSide()) Stacks.setData(stack, MirrorEnchanting.cycle(Stacks.toDomain(stack)).data());
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (!level.isClientSide()) {
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.0f);
            if (!player.isCreative()) stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            if (!stack.isEmpty()) {
                player.openMenu(new SimpleMenuProvider((cid, inv, p) -> new MirrorMenu(cid, inv, hand),
                        Component.translatable("container.mariculture.mirror")));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
        java.util.function.Consumer<Component> lines = list::add;
        if (MirrorEnchanting.range(Stacks.id(this)) == null) return;
        int[] l = MirrorEnchanting.levels(Stacks.toDomain(stack));
        lines.accept(Component.translatable("item.mariculture.mirror.levels", l[0], l[2]).withStyle(ChatFormatting.GRAY));
    }
}

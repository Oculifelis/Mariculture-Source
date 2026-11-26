package mariculture.platform.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
    public MirrorItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String id = Stacks.id(this);
        if (player.isShiftKeyDown()) {
            if (MirrorEnchanting.range(id) == null) return InteractionResult.PASS;
            if (!level.isClientSide()) Stacks.setData(stack, MirrorEnchanting.cycle(Stacks.toDomain(stack)).data());
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.0f);
            if (!player.isCreative()) stack.hurtAndBreak(1, player, hand);
            if (!stack.isEmpty()) {
                player.openMenu(new SimpleMenuProvider((cid, inv, p) -> new MirrorMenu(cid, inv, hand),
                        Component.translatable("container.mariculture.mirror")));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        if (MirrorEnchanting.range(Stacks.id(this)) == null) return;
        int[] l = MirrorEnchanting.levels(Stacks.toDomain(stack));
        lines.accept(Component.translatable("item.mariculture.mirror.levels", l[0], l[2]).withStyle(ChatFormatting.GRAY));
    }
}

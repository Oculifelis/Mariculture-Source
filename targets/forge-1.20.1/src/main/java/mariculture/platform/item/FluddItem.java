package mariculture.platform.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import mariculture.domain.machine.Stack;
import mariculture.domain.maritech.Fludd;
import mariculture.domain.maritech.FluddStand;
import mariculture.platform.ModContent;
import mariculture.platform.Stacks;
import mariculture.platform.machine.DirectionalMachineBlock;
import mariculture.platform.machine.MachineBlockEntity;

/** The F.L.U.D.D. (domain Fludd): worn on the chest; used on a block it becomes a stand facing away from that face. */
public class FluddItem extends ArmorItem {
    public FluddItem(ArmorMaterial material, Type type, Properties props) {
        super(material, type, props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        Direction face = ctx.getClickedFace();
        BlockPos pos = ctx.getClickedPos();
        if (!level.getBlockState(pos).canBeReplaced()) pos = pos.relative(face);
        ItemStack stack = ctx.getItemInHand();
        if (!level.getBlockState(pos).canBeReplaced() || player != null && !player.mayUseItemAt(pos, face, stack)) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            BlockState st = ModContent.block("fludd_stand").defaultBlockState();
            if (st.hasProperty(DirectionalMachineBlock.FACING_ALL)) st = st.setValue(DirectionalMachineBlock.FACING_ALL, face);
            level.setBlockAndUpdate(pos, st);
            if (level.getBlockEntity(pos) instanceof MachineBlockEntity be && be.machine() instanceof FluddStand s) {
                s.placedFrom(Stacks.toDomain(stack));
                be.setChanged();
            }
            level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Fludd.bar(Stacks.toDomain(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x40C0FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> lines, TooltipFlag flag) {
        Stack s = Stacks.toDomain(stack);
        lines.add(Component.translatable("item.mariculture.fludd.water", Fludd.water(s)).withStyle(ChatFormatting.GRAY));
        Fludd.Mode mode = Fludd.mode(s);
        if (mode != Fludd.Mode.NONE) {
            lines.add(Component.translatable("item.mariculture.fludd.mode", Component.translatable("fludd.mariculture.name." + mode.ordinal()))
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("item.mariculture.fludd.place").withStyle(ChatFormatting.DARK_GRAY));
    }
}

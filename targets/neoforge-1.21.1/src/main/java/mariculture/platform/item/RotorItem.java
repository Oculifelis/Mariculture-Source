package mariculture.platform.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import mariculture.domain.machine.Stack;
import mariculture.domain.maritech.Rotor;
import mariculture.platform.Stacks;
import mariculture.platform.machine.MachineBlock;
import mariculture.platform.machine.MachineBlockEntity;

/** Copper, aluminium and titanium rotors (domain Rotor): used on base iron they become a rotor block that keeps their wear. */
public class RotorItem extends Item {
    public RotorItem(Properties props) {
        super(props);
    }

    private Rotor.Tier tier() {
        return Rotor.tierOfItem(Stacks.id(this));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        Rotor.Tier tier = tier();
        if (tier == null || player != null && player.isShiftKeyDown()
                || !BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString().equals(Rotor.BASE)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            BlockState st = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("mariculture:" + tier.block())).defaultBlockState();
            if (st.hasProperty(MachineBlock.FACING)) st = st.setValue(MachineBlock.FACING, ctx.getHorizontalDirection().getOpposite());
            level.setBlockAndUpdate(pos, st);
            if (level.getBlockEntity(pos) instanceof MachineBlockEntity be && be.machine() instanceof Rotor r) {
                r.placedFrom(Stacks.toDomain(ctx.getItemInHand()));
                be.setChanged();
            }
            ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return Rotor.wear(Stacks.toDomain(stack)) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Rotor.bar(Stacks.toDomain(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x40C0FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) {
        Stack s = Stacks.toDomain(stack);
        Rotor.Tier tier = tier();
        if (tier == null) return;
        lines.add(Component.translatable("item.mariculture.rotor.wear", Rotor.wear(s), tier.maxWear()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.mariculture.rotor.place").withStyle(ChatFormatting.DARK_GRAY));
    }
}

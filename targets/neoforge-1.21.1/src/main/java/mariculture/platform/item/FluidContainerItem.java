package mariculture.platform.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import mariculture.domain.content.ItemKind;
import mariculture.domain.fluid.HeatBottle;
import mariculture.platform.machine.MachineBlockEntity;
import net.minecraft.world.InteractionResultHolder;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

import mariculture.platform.ModContent;

/** Ladle, titanium bucket and heat bottle: one fluid up to a capacity (domain ItemKind.FluidContainer, HeatBottle). */
public class FluidContainerItem extends Item {
    private final ItemKind.FluidContainer kind;

    public FluidContainerItem(Properties props, ItemKind.FluidContainer kind) {
        super(props);
        this.kind = kind;
    }

    private int limit(String fluid) {
        return kind.heatBottle() ? HeatBottle.volume(fluid) : kind.capacity();
    }

    /** The item fluid capability (registered in ModContent). */
    public FluidHandlerItemStack handler(ItemStack stack) {
        return new FluidHandlerItemStack(ModContent::fluidContent, stack, kind.capacity()) {
            @Override
            public boolean canFillFluidType(FluidStack fluid) {
                return limit(MachineBlockEntity.fluidId(fluid.getFluid())) > 0;
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty()) return 0;
                int room = limit(MachineBlockEntity.fluidId(resource.getFluid())) - getFluid().getAmount();
                if (room <= 0) return 0;
                return super.fill(resource.copyWithAmount(Math.min(room, resource.getAmount())), action);
            }
        };
    }

    private static FluidStack contained(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        FluidStack in = contained(stack);
        if (kind.heatBottle() && !in.isEmpty() && HeatBottle.drinkable(MachineBlockEntity.fluidId(in.getFluid())) && player.isHurt()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        ItemStack one = stack.copyWithCount(1);
        FluidActionResult r = FluidUtil.tryPickUpFluid(one, player, level, hit.getBlockPos(), hit.getDirection());
        if (!r.isSuccess() && !in.isEmpty()) r = FluidUtil.tryPlaceFluid(player, level, hand, hit.getBlockPos().relative(hit.getDirection()), one, in);
        if (!r.isSuccess()) return InteractionResultHolder.pass(stack);
        return InteractionResultHolder.sidedSuccess(r.getResult(), level.isClientSide);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 24;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) entity.heal(HeatBottle.FISH_OIL_HEAL);
        return new ItemStack(this);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        FluidStack f = contained(stack);
        if (!f.isEmpty()) lines.add(f.getHoverName().copy().withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal(f.getAmount() + " / " + (f.isEmpty() ? kind.capacity() : limit(MachineBlockEntity.fluidId(f.getFluid()))) + " mB")
                .withStyle(ChatFormatting.GRAY));
    }
}

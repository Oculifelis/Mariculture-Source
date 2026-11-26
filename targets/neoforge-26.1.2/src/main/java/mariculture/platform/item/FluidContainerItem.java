package mariculture.platform.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import mariculture.domain.content.ItemKind;
import mariculture.domain.fluid.HeatBottle;
import mariculture.platform.machine.MachineBlockEntity;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;

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
    public ResourceHandler<FluidResource> handler(ItemAccess access) {
        return new ItemAccessFluidHandler(access, ModContent.fluidContent(), kind.capacity()) {
            @Override
            public boolean isValid(int index, FluidResource resource) {
                return limit(MachineBlockEntity.fluidId(resource.getFluid())) > 0;
            }

            @Override
            protected int getCapacity(int index, FluidResource resource) {
                return resource.isEmpty() ? kind.capacity() : limit(MachineBlockEntity.fluidId(resource.getFluid()));
            }
        };
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        FluidStack in = FluidUtil.getFirstStackContained(stack);
        if (kind.heatBottle() && !in.isEmpty() && HeatBottle.drinkable(MachineBlockEntity.fluidId(in.getFluid())) && player.isHurt()) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        ResourceHandler<FluidResource> h = ItemAccess.forPlayerInteraction(player, hand).getCapability(Capabilities.Fluid.ITEM);
        if (h == null) return InteractionResult.PASS;
        if (!FluidUtil.tryPickupFluid(h, player, level, hit.getBlockPos(), hit.getDirection()).isEmpty()) return InteractionResult.SUCCESS;
        if (!in.isEmpty() && !FluidUtil.tryPlaceFluid(h, player, level, hand, hit.getBlockPos().relative(hit.getDirection())).isEmpty()) return InteractionResult.SUCCESS;
        return InteractionResult.PASS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 24;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) entity.heal(HeatBottle.FISH_OIL_HEAL);
        return new ItemStack(this);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        FluidStack f = FluidUtil.getFirstStackContained(stack);
        if (!f.isEmpty()) lines.accept(f.getHoverName().copy().withStyle(ChatFormatting.GRAY));
        lines.accept(Component.literal(f.getAmount() + " / " + (f.isEmpty() ? kind.capacity() : limit(MachineBlockEntity.fluidId(f.getFluid()))) + " mB")
                .withStyle(ChatFormatting.GRAY));
    }
}

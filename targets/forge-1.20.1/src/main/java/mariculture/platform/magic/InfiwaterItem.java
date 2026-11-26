package mariculture.platform.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import mariculture.domain.magic.WaterItems;

/** Infiwater (domain WaterItems): places water like a bucket that never empties, and gives tanks endless water. */
public class InfiwaterItem extends Item {
    public InfiwaterItem(Properties props) {
        super(props);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
        LazyOptional<IFluidHandlerItem> handler = LazyOptional.of(() -> handler(stack));
        return new ICapabilityProvider() {
            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
                return ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(cap, handler);
            }
        };
    }

    /** The item fluid capability: 1000 mB of water that never runs out. */
    public static IFluidHandlerItem handler(ItemStack container) {
        return new IFluidHandlerItem() {
            @Override
            public ItemStack getContainer() {
                return container;
            }

            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return new FluidStack(Fluids.WATER, WaterItems.INFIWATER_AMOUNT);
            }

            @Override
            public int getTankCapacity(int tank) {
                return WaterItems.INFIWATER_AMOUNT;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return stack.getFluid() == Fluids.WATER;
            }

            @Override
            public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
                return 0;
            }

            @Override
            public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
                return resource.getFluid() == Fluids.WATER ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
                int n = WaterItems.infiwaterGives(WaterItems.INFIWATER_FLUID, maxDrain);
                return n <= 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, n);
            }
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        BlockPos pos = hit.getBlockPos();
        BlockPos place = level.getBlockState(pos).getBlock() instanceof LiquidBlockContainer ? pos : pos.relative(hit.getDirection());
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(place, hit.getDirection(), stack)) return InteractionResultHolder.fail(stack);
        return ((BucketItem) Items.WATER_BUCKET).emptyContents(player, level, place, hit) ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide()) : InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable Level world, java.util.List<Component> list,
                                net.minecraft.world.item.TooltipFlag flag) {
        java.util.function.Consumer<Component> lines = list::add;
        lines.accept(Component.translatable("item.mariculture.infiwater.tooltip").withStyle(ChatFormatting.GRAY));
    }
}

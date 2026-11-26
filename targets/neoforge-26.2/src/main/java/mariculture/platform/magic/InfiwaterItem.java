package mariculture.platform.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import mariculture.domain.magic.WaterItems;

/** Infiwater (domain WaterItems): places water like a bucket that never empties, and gives tanks endless water. */
public class InfiwaterItem extends Item {
    /** Built on first use: fluid resources cannot be made while items register. */
    private static FluidResource water() {
        return FluidResource.of(Fluids.WATER);
    }

    public InfiwaterItem(Properties props) {
        super(props);
    }

    /** The item fluid capability (registered in ModContent): 1000 mB of water that never runs out. */
    public static ResourceHandler<FluidResource> handler() {
        return new ResourceHandler<>() {
            @Override
            public int size() {
                return 1;
            }

            @Override
            public FluidResource getResource(int index) {
                return water();
            }

            @Override
            public long getAmountAsLong(int index) {
                return WaterItems.INFIWATER_AMOUNT;
            }

            @Override
            public long getCapacityAsLong(int index, FluidResource resource) {
                return WaterItems.INFIWATER_AMOUNT;
            }

            @Override
            public boolean isValid(int index, FluidResource resource) {
                return resource.equals(water());
            }

            @Override
            public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
                return 0;
            }

            @Override
            public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
                return resource.equals(water()) ? WaterItems.infiwaterGives(WaterItems.INFIWATER_FLUID, amount) : 0;
            }
        };
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos();
        BlockPos place = level.getBlockState(pos).getBlock() instanceof LiquidBlockContainer ? pos : pos.relative(hit.getDirection());
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(place, hit.getDirection(), stack)) return InteractionResult.FAIL;
        return ((BucketItem) Items.WATER_BUCKET).emptyContents(player, level, place, hit) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        lines.accept(Component.translatable("item.mariculture.infiwater.tooltip").withStyle(ChatFormatting.GRAY));
    }
}

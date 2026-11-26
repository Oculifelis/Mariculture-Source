package mariculture.platform.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import mariculture.platform.ModContent;
import mariculture.platform.entity.SpeedBoatEntity;

/**
 * maritech/items/ItemSpeedBoat.java: used on water it puts a speed boat there. Changed: the boat faces the way the
 * player looks (legacy always faced the same way) and is not placed where it would collide.
 */
public class SpeedBoatItem extends Item {
    public SpeedBoatItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos();
        SpeedBoatEntity boat = ModContent.speedBoatType().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (boat == null) return InteractionResult.FAIL;
        boat.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        boat.setYRot(player.getYRot() - 90);
        if (!level.noCollision(boat, boat.getBoundingBox())) return InteractionResult.FAIL;
        if (level instanceof ServerLevel) {
            level.addFreshEntity(boat);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}

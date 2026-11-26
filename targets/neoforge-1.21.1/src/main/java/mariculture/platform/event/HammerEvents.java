package mariculture.platform.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import mariculture.domain.metal.Hammer;
import mariculture.platform.item.HammerItem;
import mariculture.platform.machine.MachineBlockEntity;

/** The hammer also breaks matching blocks around the one mined (ItemHammer.onBlockDestroyed, domain Hammer.area). */
public final class HammerEvents {
    private static boolean breaking;

    private HammerEvents() {}

    public static void onBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (breaking || player == null || !(event.getLevel() instanceof net.minecraft.world.level.Level level) || level.isClientSide) return;
        ItemStack hammer = player.getMainHandItem();
        if (!(hammer.getItem() instanceof HammerItem)) return;
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity) return;
        if (!hammer.isCorrectToolForDrops(state)) return;
        Direction look = player.getDirection();
        breaking = true;
        try {
            for (int[] o : Hammer.area(look.getStepX(), look.getStepZ())) {
                BlockPos p = pos.offset(o[0], o[1], o[2]);
                BlockState other = level.getBlockState(p);
                if (other.getBlock() != state.getBlock() || other.getDestroySpeed(level, p) < 0) continue;
                level.destroyBlock(p, !player.getAbilities().instabuild, player);
            }
        } finally {
            breaking = false;
        }
    }
}

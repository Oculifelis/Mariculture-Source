package mariculture.platform.item;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import mariculture.domain.fish.Species;
import mariculture.domain.fishing.FishingRules;
import mariculture.domain.plugin.BoundRod;
import mariculture.platform.Rngs;
import mariculture.platform.Stacks;
import mariculture.platform.event.FisheryEvents;

/** The bound fishing rod's own uses (ItemBoundRod.onItemRightClick): sneak toggles it, activated it mass-fishes. */
final class BoundRodUse {
    private BoundRodUse() {}

    static boolean active(ItemStack rod) {
        return Boolean.TRUE.equals(Stacks.data(rod).get(BoundRod.ACTIVE));
    }

    /** Returns null when the use is an ordinary cast, else whether it did something. */
    static Boolean use(Level level, Player player, InteractionHand hand, ItemStack rod) {
        if (player.fishing != null) return null;
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                Map<String, Object> data = new HashMap<>(Stacks.data(rod));
                boolean now = !active(rod);
                data.put(BoundRod.ACTIVE, now);
                Stacks.setData(rod, data);
                player.displayClientMessage(Component.translatable(now ? "item.mariculture.bound_fishing_rod.activated"
                        : "item.mariculture.bound_fishing_rod.deactivated"), true);
            }
            return true;
        }
        if (!active(rod)) return null;
        var inv = player.getInventory();
        int slot = BoundRod.shardSlot(inv.selected, i -> BoundRod.shardLimit(Stacks.id(inv.getItem(i).getItem())) > 0);
        if (slot < 0) return false;
        if (level instanceof ServerLevel server) {
            int limit = BoundRod.shardLimit(Stacks.id(inv.getItem(slot).getItem()));
            BlockPos origin = player.blockPosition();
            var result = BoundRod.catches(p -> server.getFluidState(origin.offset(BoundRod.dx(p), BoundRod.dy(p), BoundRod.dz(p))).is(FluidTags.WATER),
                    limit, Rngs.of(server.random));
            if (result.lightning() != null) {
                var bolt = EntityType.LIGHTNING_BOLT.create(server);
                if (bolt != null) {
                    bolt.moveTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(origin.offset(result.lightning()[0], result.lightning()[1], result.lightning()[2])));
                    server.addFreshEntity(bolt);
                }
            }
            for (int[] o : result.catches()) {
                BlockPos at = origin.offset(o[0], o[1], o[2]);
                FishingRules.Catch c = FisheryEvents.cast(server, at, player, rod, Species.Rod.BLOOD);
                ItemStack caught = FisheryEvents.resolveCatch(c, server, net.minecraft.world.phys.Vec3.atCenterOf(at), rod, null);
                if (caught.isEmpty()) continue;
                float f = 0.7f;
                ItemEntity item = new ItemEntity(server, at.getX() + server.random.nextFloat() * f + (1 - f) * 0.5,
                        at.getY() + server.random.nextFloat() * f + (1 - f) * 0.5, at.getZ() + server.random.nextFloat() * f + (1 - f) * 0.5, caught);
                item.setDeltaMovement(0, server.random.nextFloat() * f * 1.5f, 0);
                item.setNoPickUpDelay();
                server.addFreshEntity(item);
                player.awardStat(Stats.FISH_CAUGHT, 1);
                server.addFreshEntity(new ExperienceOrb(server, player.getX(), player.getY() + 0.5, player.getZ() + 0.5, server.random.nextInt(6) + 1));
            }
            if (!player.getAbilities().instabuild) {
                inv.getItem(slot).shrink(1);
                // Legacy drew 500 LP from the soul network per fish; without Blood Magic's API the rod wears instead.
                rod.hurtAndBreak(result.catches().size(), player, p -> p.broadcastBreakEvent(hand));
            }
        }
        return true;
    }
}

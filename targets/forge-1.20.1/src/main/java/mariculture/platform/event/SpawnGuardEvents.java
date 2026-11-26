package mariculture.platform.event;

import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;

import mariculture.domain.fishery.SpawnGuard;

/** Hostile spawns inside a Stargazer feeder's box fail (rules in {@link SpawnGuard}). */
public final class SpawnGuardEvents {
    private SpawnGuardEvents() {}

    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getEntity().getType().getCategory() != MobCategory.MONSTER) return;
        String dim = event.getLevel().getLevel().dimension().location().toString();
        if (SpawnGuard.blocks(dim, (int) Math.floor(event.getX()), (int) Math.floor(event.getY()), (int) Math.floor(event.getZ()))) {
            event.setResult(Event.Result.DENY);
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        SpawnGuard.clear();
    }
}

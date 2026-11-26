package mariculture.platform.event;

import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import mariculture.domain.fishery.SpawnGuard;

/** Hostile spawns inside a Stargazer feeder's box fail (rules in {@link SpawnGuard}). */
public final class SpawnGuardEvents {
    private SpawnGuardEvents() {}

    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getEntity().getType().getCategory() != MobCategory.MONSTER) return;
        String dim = event.getLevel().getLevel().dimension().identifier().toString();
        if (SpawnGuard.blocks(dim, (int) Math.floor(event.getX()), (int) Math.floor(event.getY()), (int) Math.floor(event.getZ()))) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        SpawnGuard.clear();
    }
}

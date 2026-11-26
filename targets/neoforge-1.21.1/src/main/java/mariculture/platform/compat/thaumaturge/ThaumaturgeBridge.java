package mariculture.platform.compat.thaumaturge;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/**
 * Calls Thaumaturge's aura and knowledge API by name, since Thaumaturge is on no maven and stays optional. Every
 * method returns the "mod missing" answer (0 or false) when Thaumaturge is absent or its API changed.
 */
public final class ThaumaturgeBridge {
    private static final String API = "com.leclowndu93150.thaumaturge.api.";
    private static boolean tried;
    private static MethodHandle capacityRemaining, addVis, drainFlux, knowledgeOf, researchStage, researchComplete;

    private ThaumaturgeBridge() {}

    private static synchronized boolean ready() {
        if (!tried) {
            tried = true;
            if (!ModList.get().isLoaded("thaumaturge")) return false;
            try {
                MethodHandles.Lookup l = MethodHandles.publicLookup();
                Class<?> aura = Class.forName(API + "aura.AuraHelper");
                Class<?> access = Class.forName(API + "capability.KnowledgeAccess");
                Class<?> knowledge = Class.forName(API + "capability.IPlayerKnowledge");
                capacityRemaining = l.findStatic(aura, "capacityRemaining", MethodType.methodType(float.class, Level.class, BlockPos.class));
                addVis = l.findStatic(aura, "addVis", MethodType.methodType(void.class, Level.class, BlockPos.class, float.class));
                drainFlux = l.findStatic(aura, "drainFlux", MethodType.methodType(float.class, Level.class, BlockPos.class, float.class, boolean.class));
                knowledgeOf = l.findStatic(access, "of", MethodType.methodType(knowledge, Player.class));
                researchStage = l.findVirtual(knowledge, "researchStage", MethodType.methodType(int.class, ResourceLocation.class));
                researchComplete = l.findVirtual(knowledge, "isResearchComplete", MethodType.methodType(boolean.class, ResourceLocation.class));
            } catch (ReflectiveOperationException | LinkageError e) {
                capacityRemaining = null;
            }
        }
        return capacityRemaining != null;
    }

    /** Adds vis to the aura at {@code pos}, up to what the chunk still holds; returns what was added. */
    public static float addVis(Level level, BlockPos pos, float amount) {
        if (amount <= 0 || !ready()) return 0;
        try {
            float added = Math.min(amount, Math.max(0, (float) capacityRemaining.invokeExact(level, pos)));
            if (added > 0) addVis.invokeExact(level, pos, added);
            return added;
        } catch (Throwable t) {
            return 0;
        }
    }

    /** Drains flux from the aura at {@code pos}; returns what was drained. */
    public static float drainFlux(Level level, BlockPos pos, float amount) {
        if (amount <= 0 || !ready()) return 0;
        try {
            return (float) drainFlux.invokeExact(level, pos, amount, false);
        } catch (Throwable t) {
            return 0;
        }
    }

    /**
     * Whether the online player with this UUID got past the first stage of a research entry. The fish entries ask
     * for the fish itself in their last stage, so breeding needs only the stage before it. False when offline.
     */
    public static boolean researchPastFirstStage(ServerLevel level, String uuid, String entry) {
        if (uuid == null || entry == null || !ready()) return false;
        try {
            Player player = level.getServer().getPlayerList().getPlayer(UUID.fromString(uuid));
            if (player == null) return false;
            Object k = knowledgeOf.invoke(player);
            if (k == null) return false;
            ResourceLocation id = ResourceLocation.parse(entry);
            return (boolean) researchComplete.invoke(k, id) || (int) researchStage.invoke(k, id) >= 1;
        } catch (Throwable t) {
            return false;
        }
    }
}

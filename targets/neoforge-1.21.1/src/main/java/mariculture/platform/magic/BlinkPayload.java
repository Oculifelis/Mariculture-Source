package mariculture.platform.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import mariculture.domain.content.Ids;
import mariculture.domain.magic.Blink;
import mariculture.domain.magic.JewelryEnchantment;

/** The client asks to blink onto the block it aims at (legacy PacketTeleport); the server checks everything. */
public record BlinkPayload(BlockPos target) implements CustomPacketPayload {
    public static final Type<BlinkPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ids.MOD_ID, "blink"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlinkPayload> CODEC = BlockPos.STREAM_CODEC.map(BlinkPayload::new, BlinkPayload::target).cast();

    @Override
    public Type<BlinkPayload> type() {
        return TYPE;
    }

    private static final java.util.Map<java.util.UUID, Long> LAST = new java.util.concurrent.ConcurrentHashMap<>();

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, BlinkPayload::handle);
    }

    private static void handle(BlinkPayload msg, IPayloadContext ctx) {
        Player p = ctx.player();
        int strength = WornStore.strength(p, JewelryEnchantment.BLINK.id());
        BlockPos pos = msg.target();
        if (strength <= 0 || !p.level().isLoaded(pos) || !p.level().isEmptyBlock(pos.above()) || !p.level().isEmptyBlock(pos.above(2))) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 1, z = pos.getZ() + 0.5;
        if (!Blink.inRange(x - p.getX(), y - p.getY(), z - p.getZ(), strength)) return;
        if (!Blink.cooledDown(System.currentTimeMillis() - LAST.getOrDefault(p.getUUID(), 0L))) return;
        LAST.put(p.getUUID(), System.currentTimeMillis());
        p.level().playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        p.teleportTo(x, y, z);
        p.resetFallDistance();
        p.level().playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        WornStore.wear(p, JewelryEnchantment.BLINK.id(), 1, null);
    }
}

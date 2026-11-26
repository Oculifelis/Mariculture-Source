package mariculture.platform.magic;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import mariculture.domain.content.Ids;
import mariculture.domain.magic.Blink;
import mariculture.domain.magic.JewelryEnchantment;

/** The client asks to blink onto the block it aims at (legacy PacketTeleport); the server checks everything. */
public record BlinkPacket(BlockPos target) {
    private static final String VERSION = "1";
    /** The mod channel (blink is message 0, the F.L.U.D.D. message 1). */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(Ids.MOD_ID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private static final java.util.Map<java.util.UUID, Long> LAST = new java.util.concurrent.ConcurrentHashMap<>();

    public static void register() {
        CHANNEL.messageBuilder(BlinkPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((m, buf) -> buf.writeBlockPos(m.target()))
                .decoder((FriendlyByteBuf buf) -> new BlinkPacket(buf.readBlockPos()))
                .consumerMainThread(BlinkPacket::handle)
                .add();
    }

    public static void send(BlockPos target) {
        CHANNEL.sendToServer(new BlinkPacket(target));
    }

    private static void handle(BlinkPacket msg, Supplier<NetworkEvent.Context> ctx) {
        Player p = ctx.get().getSender();
        ctx.get().setPacketHandled(true);
        if (p == null) return;
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

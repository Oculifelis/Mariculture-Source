package mariculture.platform.item;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import mariculture.domain.machine.Stack;
import mariculture.domain.maritech.Fludd;
import mariculture.platform.ModContent;
import mariculture.platform.Stacks;
import mariculture.platform.entity.FluddSquirt;
import mariculture.platform.magic.BlinkPacket;

/**
 * The client tells the server what the F.L.U.D.D. on its chest did (legacy PacketFLUDD): switch mode, use a mode for a
 * tick, squirt, or land from a hover. The server keeps the water and forgives the flight and the fall.
 */
public record FluddPacket(int action, int mode) {
    public static final int SWITCH = 0, USE = 1, SQUIRT = 2, LAND = 3;
    private static final String FLOAT = "mariculture_fludd_float";

    public static void register() {
        BlinkPacket.CHANNEL.messageBuilder(FluddPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder((m, buf) -> {
                    buf.writeVarInt(m.action());
                    buf.writeVarInt(m.mode());
                })
                .decoder((FriendlyByteBuf buf) -> new FluddPacket(buf.readVarInt(), buf.readVarInt()))
                .consumerMainThread(FluddPacket::handle)
                .add();
    }

    public static void send(int action, int mode) {
        BlinkPacket.CHANNEL.sendToServer(new FluddPacket(action, mode));
    }

    private static void handle(FluddPacket msg, Supplier<NetworkEvent.Context> ctx) {
        Player p = ctx.get().getSender();
        ctx.get().setPacketHandled(true);
        if (p == null) return;
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        if (!Stacks.id(chest.getItem()).equals(Fludd.ITEM)) return;
        Stack fludd = Stacks.toDomain(chest);
        if (msg.action() == SWITCH) {
            Stack next = Fludd.nextMode(fludd);
            Stacks.setData(chest, next.data());
            p.sendSystemMessage(Component.translatable("fludd.mariculture.mode." + next.intData(Fludd.MODE, 0)));
            return;
        }
        if (msg.action() == LAND) {
            p.resetFallDistance();
            allowFloat(p);
            return;
        }
        Fludd.Mode mode = msg.mode() >= 0 && msg.mode() < 4 ? Fludd.Mode.values()[msg.mode()] : Fludd.Mode.HOVER;
        if (Fludd.water(fludd) <= 0) return;
        Stacks.setData(chest, Fludd.use(fludd, mode).data());
        if (msg.action() == SQUIRT && p.level() instanceof ServerLevel server) {
            server.playSound(null, p.blockPosition(), ModContent.sound(Fludd.SOUND), SoundSource.PLAYERS, 1.0f, 1.0f);
            FluddSquirt squirt = new FluddSquirt(server, p);
            squirt.setPos(squirt.getX(), squirt.getY() + 0.35, squirt.getZ());
            squirt.shootFromRotation(p, p.getXRot(), p.getYRot(), 0.0f, 1.5f, 1.0f);
            server.addFreshEntity(squirt);
            return;
        }
        if (mode != Fludd.Mode.TURBO) p.resetFallDistance();
        allowFloat(p);
    }

    /**
     * Lets the server accept a hovering or rocketing player for 10 ticks after the last use (no kick for floating):
     * legacy flew the player on its client only, which the server would kick after 80 ticks without allow-flight.
     */
    private static void allowFloat(Player p) {
        if (p.getAbilities().mayfly && !p.getPersistentData().contains(FLOAT)) return;
        p.getAbilities().mayfly = true;
        p.getPersistentData().putLong(FLOAT, p.level().getGameTime());
    }

    /** Takes the float back once the F.L.U.D.D. has not been used for 10 ticks. */
    public static void tickFloat(Player p) {
        if (p.level().isClientSide() || !p.getPersistentData().contains(FLOAT)) return;
        if (p.level().getGameTime() - p.getPersistentData().getLong(FLOAT) <= 10) return;
        p.getPersistentData().remove(FLOAT);
        if (!p.isCreative() && !p.isSpectator()) {
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
        }
    }
}

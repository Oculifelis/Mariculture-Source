package mariculture.platform.item;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import mariculture.domain.content.Ids;
import mariculture.domain.machine.Stack;
import mariculture.domain.maritech.Fludd;
import mariculture.platform.ModContent;
import mariculture.platform.Stacks;
import mariculture.platform.entity.FluddSquirt;

/**
 * The client tells the server what the F.L.U.D.D. on its chest did (legacy PacketFLUDD): switch mode, use a mode for a
 * tick, squirt, or land from a hover. The server keeps the water and forgives the flight and the fall.
 */
public record FluddPayload(int action, int mode) implements CustomPacketPayload {
    public static final int SWITCH = 0, USE = 1, SQUIRT = 2, LAND = 3;
    public static final Type<FluddPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Ids.MOD_ID, "fludd"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FluddPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FluddPayload::action, ByteBufCodecs.VAR_INT, FluddPayload::mode, FluddPayload::new);

    @Override
    public Type<FluddPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, FluddPayload::handle);
    }

    private static void handle(FluddPayload msg, IPayloadContext ctx) {
        Player p = ctx.player();
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
            if (p instanceof ServerPlayer sp) sp.connection.resetFlyingTicks();
            return;
        }
        Fludd.Mode mode = msg.mode() >= 0 && msg.mode() < 4 ? Fludd.Mode.values()[msg.mode()] : Fludd.Mode.HOVER;
        if (Fludd.water(fludd) <= 0) return;
        Stacks.setData(chest, Fludd.use(fludd, mode).data());
        if (msg.action() == SQUIRT && p.level() instanceof ServerLevel server) {
            server.playSound(null, p.blockPosition(), ModContent.sound(Fludd.SOUND), SoundSource.PLAYERS, 1.0f, 1.0f);
            FluddSquirt squirt = new FluddSquirt(server, p);
            squirt.setPos(squirt.getX(), squirt.getY() + 0.35, squirt.getZ());
            Projectile.spawnProjectile(squirt, server, chest, s -> s.shootFromRotation(p, p.getXRot(), p.getYRot(), 0.0f, 1.5f, 1.0f));
            return;
        }
        if (mode != Fludd.Mode.TURBO) p.resetFallDistance();
        if (p instanceof ServerPlayer sp) sp.connection.resetFlyingTicks();
    }
}

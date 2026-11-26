package mariculture.platform.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

import mariculture.domain.maritech.Fludd;
import mariculture.platform.Stacks;
import mariculture.platform.item.FluddPayload;

/**
 * The F.L.U.D.D. controls (legacy FLUDDEvents): the activate key switches mode while sneaking, squirts in squirt mode,
 * and held moves the player by {@link Fludd.Pilot}; the server is told each tick that costs water or forgives a fall.
 */
final class FluddClient {
    private static final Fludd.Pilot PILOT = new Fludd.Pilot();
    private static boolean wasDown;

    private FluddClient() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick(Minecraft.getInstance()));
    }

    private static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        boolean down = p != null && mc.screen == null && BlinkKey.KEY.isDown();
        boolean pressed = down && !wasDown;
        wasDown = down;
        if (p == null) return;
        var chest = p.getItemBySlot(EquipmentSlot.CHEST);
        if (!Stacks.id(chest.getItem()).equals(Fludd.ITEM)) return;
        Fludd.Mode mode = Fludd.mode(Stacks.toDomain(chest));
        if (pressed && p.isShiftKeyDown()) {
            PacketDistributor.sendToServer(new FluddPayload(FluddPayload.SWITCH, 0));
            return;
        }
        if (pressed && mode == Fludd.Mode.SQUIRT) PacketDistributor.sendToServer(new FluddPayload(FluddPayload.SQUIRT, mode.ordinal()));
        boolean held = down && !p.isShiftKeyDown();
        Fludd.Step s = PILOT.tick(mode, new Fludd.Input(held, p.onGround(), p.isInWater(), p.input.up, p.input.jumping));
        Vec3 v = p.getDeltaMovement();
        double y = v.y + s.addY();
        if (!Double.isNaN(s.setY())) y = s.setY();
        if (y != v.y) p.setDeltaMovement(v.x, y, v.z);
        if (s.forward() > 0) p.moveRelative((float) s.forward(), new Vec3(0, 0, 1));
        if (s.noFall()) p.resetFallDistance();
        if (s.cost()) PacketDistributor.sendToServer(new FluddPayload(FluddPayload.USE, mode.ordinal()));
        else if (s.noFall()) PacketDistributor.sendToServer(new FluddPayload(FluddPayload.LAND, mode.ordinal()));
        if (held && (s.cost() || s.noFall())) {
            boolean turbo = mode == Fludd.Mode.TURBO;
            for (int i = 0; i < 3; i++) {
                p.level().addParticle(turbo ? ParticleTypes.CLOUD : mode == Fludd.Mode.ROCKET ? ParticleTypes.POOF : ParticleTypes.CLOUD,
                        p.getX() + (p.getRandom().nextDouble() - 0.5) * 0.4, p.getY() + (turbo ? 0.8 : -0.2), p.getZ() + (p.getRandom().nextDouble() - 0.5) * 0.4,
                        0, -0.3, 0);
            }
        }
    }
}

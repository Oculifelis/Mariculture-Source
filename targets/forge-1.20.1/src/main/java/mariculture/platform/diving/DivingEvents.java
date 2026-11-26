package mariculture.platform.diving;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;

import mariculture.domain.config.Settings;
import mariculture.domain.content.Ids;
import mariculture.domain.diving.Diving;

/** Diving gear effects (domain Diving): air from the snorkel, hardcore diving, boots, lifejacket, pants, snorkel anvil rule. */
public final class DivingEvents {
    private DivingEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(DivingEvents::onTick);
        MinecraftForge.EVENT_BUS.addListener(DivingEvents::onBreakSpeed);
        MinecraftForge.EVENT_BUS.addListener(DivingEvents::onAnvil);
    }

    private static boolean wearing(Player p, EquipmentSlot slot, String id) {
        return id(p.getItemBySlot(slot)).equals(Ids.MOD_ID + ":" + id);
    }

    private static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player p = event.player;
        boolean head = p.isEyeInFluid(FluidTags.WATER);
        if (!p.level().isClientSide) {
            long time = p.level().getGameTime();
            if (wearing(p, EquipmentSlot.HEAD, Diving.SNORKEL) && Diving.snorkelGivesAir(time, head, p.getAbilities().instabuild)) {
                p.setAirSupply(Math.min(p.getMaxAirSupply(), p.getAirSupply() + 1));
            }
            ItemStack helmet = p.getItemBySlot(EquipmentSlot.HEAD);
            boolean ours = !helmet.isEmpty() && id(helmet).startsWith(Ids.MOD_ID + ":");
            int loss = Diving.hardcoreAirLoss(Settings.HARDCORE_DIVING.get(), time, p.getAirSupply(), head, ours);
            if (loss > 0) p.setAirSupply(p.getAirSupply() - loss);
            return;
        }
        if (!p.isLocalPlayer()) return;
        Vec3 m = p.getDeltaMovement();
        if (wearing(p, EquipmentSlot.FEET, Diving.BOOTS)) {
            Diving.Motion b = Diving.boots(new Diving.Motion(m.x, m.y, m.z), p.isInWater(), p.getAbilities().flying, p.zza > 0, p.isShiftKeyDown());
            if (b != null) p.setDeltaMovement(m = new Vec3(b.x(), b.y(), b.z()));
        }
        if (wearing(p, EquipmentSlot.CHEST, Diving.LIFEJACKET)) {
            double y = Diving.lifejacket(m.y, head, p.isInWater(), p.isShiftKeyDown());
            if (!Double.isNaN(y)) p.setDeltaMovement(m.x, y, m.z);
        }
    }

    private static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player p = event.getEntity();
        boolean oyster = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()).toString().equals(Ids.MOD_ID + ":oyster");
        event.setNewSpeed(Diving.breakSpeed(event.getNewSpeed(), wearing(p, EquipmentSlot.LEGS, Diving.PANTS), p.isEyeInFluid(FluidTags.WATER),
                p.hasCorrectToolForDrops(event.getState()), oyster));
    }

    private static void onAnvil(AnvilUpdateEvent event) {
        if (!Diving.anvilAllowed(id(event.getLeft()), id(event.getRight()))) event.setCanceled(true);
    }
}

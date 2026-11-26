package mariculture.platform.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import mariculture.domain.machine.Stack;
import mariculture.domain.magic.MobMagnet;
import mariculture.domain.magic.WaterItems;
import mariculture.platform.Stacks;

/** Mob magnet binding and the hydrophobic initiator (domain MobMagnet, WaterItems). */
public final class MagicToolEvents {
    private MagicToolEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(MagicToolEvents::onDeath);
        MinecraftForge.EVENT_BUS.addListener(MagicToolEvents::onRightClickItem);
    }

    /** MagicEventHandler.onEntityDeath: a kill made while holding an unbound magnet binds it. */
    private static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        boolean direct = event.getSource().getDirectEntity() instanceof Player;
        if (!(event.getSource().getEntity() instanceof Player p)) return;
        ItemStack held = p.getMainHandItem();
        if (!(held.getItem() instanceof MobMagnetItem)) return;
        Stack bound = MobMagnet.bind(Stacks.toDomain(held), BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString(),
                event.getEntity() instanceof Player, direct);
        if (bound != null) Stacks.setData(held, bound.data());
    }

    /** MagicEventHandler.onPlayerInteract: with the initiator carried, a block used into the air goes on the water looked at. */
    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player p = event.getEntity();
        ItemStack held = event.getItemStack();
        Item initiator = Stacks.item(WaterItems.INITIATOR);
        if (!WaterItems.placesOnWater(held.getItem() instanceof BlockItem, p.getInventory().contains(new ItemStack(initiator)))) return;
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getViewVector(1.0f).scale(p.getBlockReach()));
        BlockHitResult hit = p.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, p));
        if (hit.getType() != HitResult.Type.BLOCK || p.level().getFluidState(hit.getBlockPos()).isEmpty()) return;
        BlockPos above = hit.getBlockPos().above();
        InteractionResult r = held.useOn(new UseOnContext(p, event.getHand(), hit.withPosition(above)));
        if (r.consumesAction()) {
            event.setCanceled(true);
            event.setCancellationResult(r);
        }
    }

    /** The initiator's tooltip line. */
    public static Component initiatorTooltip() {
        return Component.translatable("item.mariculture.hydrophobic_initiator.tooltip").withStyle(ChatFormatting.GRAY);
    }
}

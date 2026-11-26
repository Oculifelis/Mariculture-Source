package mariculture.platform.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

import mariculture.domain.content.Ids;
import mariculture.domain.diving.Diving;

/** ArmorClientHandler.onFogRender: the diving helmet and scuba mask clear the fog under water. */
public final class DivingClient {
    private DivingClient() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(DivingClient::onFog);
    }

    private static void onFog(ViewportEvent.RenderFog event) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        String head = BuiltInRegistries.ITEM.getKey(player.getItemBySlot(EquipmentSlot.HEAD).getItem()).toString();
        // ScubaEvent.onFogRender: the scuba mask clears water fog, and lava fog with Fire Protection on it.
        boolean mask = head.equals(Ids.MOD_ID + ":" + mariculture.domain.diving.Scuba.MASK);
        boolean clear = event.getType() == FogType.WATER && (mask || head.equals(Ids.MOD_ID + ":" + Diving.HELMET))
                || event.getType() == FogType.LAVA && mask && mariculture.platform.diving.ScubaEvents.fireProtection(player, player.getItemBySlot(EquipmentSlot.HEAD));
        if (!clear) return;
        event.setNearPlaneDistance(Diving.HELMET_FOG_NEAR);
        event.setFarPlaneDistance(Diving.HELMET_FOG_FAR);
    }
}

package mariculture.platform.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

import mariculture.domain.magic.Blink;
import mariculture.platform.magic.BlinkPayload;

/** The Blink key (legacy activate key, V): pressing it asks the server to blink onto the aimed block. */
public final class BlinkKey {
    static final KeyMapping KEY = new KeyMapping("key.mariculture.blink", InputConstants.KEY_V, "key.categories.mariculture");

    private BlinkKey() {}

    static void register(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(KEY));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            while (KEY.consumeClick()) {
                if (mc.player == null || mc.screen != null) continue;
                HitResult hit = mc.player.pick(Blink.CLIENT_LOOK, 1.0f, false);
                if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK) PacketDistributor.sendToServer(new BlinkPayload(b.getBlockPos()));
            }
        });
    }
}

package mariculture.platform.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import mariculture.platform.ModContent;
import mariculture.platform.fluid.ModFluids;

/** Client-only registration: machine screens, fluid render layers, entity renderers (fluid textures come from the fluid types). */
public final class ModClient {
    private ModClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((net.minecraftforge.client.event.RegisterColorHandlersEvent.Item e) -> e.register(mariculture.platform.item.MetalDropletItem::tint,
                mariculture.platform.Stacks.item(mariculture.domain.fishery.Droplets.METAL)));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModContent.bassBombType(), ThrownItemRenderer::new));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModContent.rockhopperType(), RockhopperRenderer::new));
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> e.registerLayerDefinition(RockhopperModel.LAYER, RockhopperModel::createLayer));
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> e.registerLayerDefinition(SpeedBoatModel.LAYER, SpeedBoatModel::createLayer));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModContent.speedBoatType(), SpeedBoatRenderer::new));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModContent.fireImmuneItemType(),
                net.minecraft.client.renderer.entity.ItemEntityRenderer::new));
        CustomBlockModel.register(modBus);
        if (net.minecraftforge.fml.ModList.get().isLoaded("forestry")) {
            modBus.addListener(mariculture.platform.compat.forestry.ForestryCompat::colors);
        }
        BlinkKey.register(modBus);
        FluddClient.register();
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModContent.fluddSquirtType(),
                net.minecraft.client.renderer.entity.NoopRenderer::new));
        DivingClient.register();
        modBus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> {
            MenuScreens.register(ModContent.machineMenu(), MachineScreen::new);
            MenuScreens.register(ModContent.mirrorMenu(), MirrorScreen::new);
            for (ModFluids.Entry f : ModFluids.entries().values()) {
                ItemBlockRenderTypes.setRenderLayer(f.source().get(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(f.flowing().get(), RenderType.translucent());
            }
        }));
    }
}

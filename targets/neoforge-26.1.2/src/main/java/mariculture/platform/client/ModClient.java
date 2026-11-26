package mariculture.platform.client;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;

import mariculture.domain.fluid.FluidEffect;
import mariculture.platform.ModContent;
import mariculture.platform.fluid.ModFluids;

/**
 * Client-only registration: machine screens, fluid models (26.1 renders fluids from registered models, and picks
 * their chunk layer from the textures), entity renderers.
 */
public final class ModClient {
    private ModClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterMenuScreensEvent e) -> e.register(ModContent.machineMenu(), MachineScreen::new));
        modBus.addListener((RegisterMenuScreensEvent e) -> e.register(ModContent.mirrorMenu(), MirrorScreen::new));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModContent.bassBombType(), ThrownItemRenderer::new));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModContent.rockhopperType(), RockhopperRenderer::new));
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> e.registerLayerDefinition(RockhopperModel.LAYER, RockhopperModel::createLayer));
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> e.registerLayerDefinition(SpeedBoatModel.LAYER, SpeedBoatModel::createLayer));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModContent.speedBoatType(), SpeedBoatRenderer::new));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModContent.fireImmuneItemType(),
                net.minecraft.client.renderer.entity.ItemEntityRenderer::new));
        modBus.addListener(ModClient::fluidModels);
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModContent.fluddSquirtType(),
                net.minecraft.client.renderer.entity.NoopRenderer::new));
        CustomBlockModel.register(modBus);
        BlinkKey.register(modBus);
        FluddClient.register();
        DivingClient.register();
        ScubaLooks.register(modBus);
    }

    private static void fluidModels(RegisterFluidModelsEvent event) {
        for (ModFluids.Entry f : ModFluids.entries().values()) {
            Material still = new Material(Identifier.parse(f.spec().stillTexture()));
            Material flow = new Material(Identifier.parse(f.spec().flowTexture()));
            // Mariculture's own fluid textures are already coloured; vanilla water reused for pressurised water is tinted blue.
            var tint = f.spec().effect() == FluidEffect.PRESSURISED ? FluidTintSources.constant(0xFF3F76E4) : null;
            event.register(new FluidModel.Unbaked(still, flow, null, tint), f.source(), f.flowing());
        }
    }
}

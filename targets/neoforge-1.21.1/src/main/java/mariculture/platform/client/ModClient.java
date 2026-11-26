package mariculture.platform.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import mariculture.domain.fluid.FluidEffect;
import mariculture.platform.ModContent;
import mariculture.platform.fluid.ModFluids;

/** Client-only registration: machine screens, fluid textures and render layers, entity renderers. */
public final class ModClient {
    private ModClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item e) -> e.register(mariculture.platform.item.MetalDropletItem::tint,
                mariculture.platform.Stacks.item(mariculture.domain.fishery.Droplets.METAL)));
        modBus.addListener((RegisterMenuScreensEvent e) -> e.register(ModContent.machineMenu(), MachineScreen::new));
        modBus.addListener((RegisterMenuScreensEvent e) -> e.register(ModContent.mirrorMenu(), MirrorScreen::new));
        CustomBlockModel.register(modBus);
        if (net.neoforged.fml.ModList.get().isLoaded("forestry")) {
            modBus.addListener(mariculture.platform.compat.forestry.ForestryCompat::colors);
        }
        BlinkKey.register(modBus);
        FluddClient.register();
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModContent.fluddSquirtType(),
                net.minecraft.client.renderer.entity.NoopRenderer::new));
        DivingClient.register();
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModContent.bassBombType(), ThrownItemRenderer::new));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModContent.rockhopperType(), RockhopperRenderer::new));
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> e.registerLayerDefinition(RockhopperModel.LAYER, RockhopperModel::createLayer));
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> e.registerLayerDefinition(SpeedBoatModel.LAYER, SpeedBoatModel::createLayer));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModContent.speedBoatType(), SpeedBoatRenderer::new));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModContent.fireImmuneItemType(),
                net.minecraft.client.renderer.entity.ItemEntityRenderer::new));
        modBus.addListener(ModClient::extensions);
        modBus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> {
            ScubaLooks.properties();
            for (ModFluids.Entry f : ModFluids.entries().values()) {
                ItemBlockRenderTypes.setRenderLayer(f.source().get(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(f.flowing().get(), RenderType.translucent());
            }
        }));
    }

    private static void extensions(RegisterClientExtensionsEvent event) {
        for (ModFluids.Entry f : ModFluids.entries().values()) {
            ResourceLocation still = ResourceLocation.parse(f.spec().stillTexture());
            ResourceLocation flow = ResourceLocation.parse(f.spec().flowTexture());
            // Mariculture's own fluid textures are already coloured; vanilla water reused for pressurised water is tinted blue.
            int tint = f.spec().effect() == FluidEffect.PRESSURISED ? 0xFF3F76E4 : 0xFFFFFFFF;
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return still;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return flow;
                }

                @Override
                public int getTintColor() {
                    return tint;
                }
            }, f.type().get());
        }
    }
}

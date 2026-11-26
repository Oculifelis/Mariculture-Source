package mariculture.platform;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

import mariculture.domain.content.Ids;
import mariculture.platform.client.ModClient;
import mariculture.platform.event.FisheryEvents;
import mariculture.platform.event.ZombieGearEvents;

/** NeoForge 1.21.1 entry point. Gameplay rules live in core-domain; this target only adapts them. */
@Mod(Ids.MOD_ID)
public final class Mariculture {
    public Mariculture(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        ModContent.register(modBus);
        PluginCondition.register(modBus);
        mariculture.platform.event.StardewCatchModifier.register(modBus);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, ModConfigs.SPEC);
        modBus.addListener(ModConfigs::onLoading);
        modBus.addListener(ModConfigs::onReloading);
        mariculture.domain.machine.ItemTags.install((item, tag) -> {
            var i = mariculture.platform.Stacks.item(item);
            return i != null && new net.minecraft.world.item.ItemStack(i).is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.parse(tag)));
        });
        NeoForge.EVENT_BUS.addListener(ZombieGearEvents::onJoin);
        NeoForge.EVENT_BUS.addListener(mariculture.platform.event.SpawnGuardEvents::onPositionCheck);
        NeoForge.EVENT_BUS.addListener(mariculture.platform.event.SpawnGuardEvents::onServerStopped);
        NeoForge.EVENT_BUS.addListener(FisheryEvents::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(FisheryEvents::onTagsUpdated);
        NeoForge.EVENT_BUS.addListener(FisheryEvents::onItemFished);
        NeoForge.EVENT_BUS.addListener(FisheryEvents::onItemExpire);
        NeoForge.EVENT_BUS.addListener(FisheryEvents::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(mariculture.platform.event.HammerEvents::onBreak);
        mariculture.platform.magic.JewelryEvents.register(modBus);
        mariculture.platform.magic.MagicToolEvents.register();
        mariculture.platform.diving.DivingEvents.register();
        mariculture.platform.diving.ScubaEvents.register();
        modBus.addListener(mariculture.platform.magic.BlinkPayload::register);
        modBus.addListener(mariculture.platform.item.FluddPayload::register);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) -> mariculture.platform.item.FluddPayload.tickFloat(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(FisheryEvents::onRegisterCommands);
        if (net.neoforged.fml.ModList.get().isLoaded("hephaestus")) {
            NeoForge.EVENT_BUS.addListener(mariculture.platform.compat.hephaestus.HephaestusImport::onServerStarted);
            NeoForge.EVENT_BUS.addListener(mariculture.platform.compat.hephaestus.HephaestusImport::onDatapackSync);
        }
        if (FMLEnvironment.dist.isClient()) ModClient.register(modBus);
    }
}

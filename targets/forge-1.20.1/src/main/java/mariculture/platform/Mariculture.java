package mariculture.platform;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import mariculture.domain.content.Ids;
import mariculture.platform.client.ModClient;
import mariculture.platform.event.FisheryEvents;
import mariculture.platform.event.ZombieGearEvents;

/** Forge 1.20.1 entry point. Gameplay rules live in core-domain; this target only adapts them. */
@Mod(Ids.MOD_ID)
public final class Mariculture {
    public Mariculture() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModContent.register(modBus);
        PluginCondition.register();
        mariculture.platform.event.StardewCatchModifier.register(modBus);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfigs.SPEC);
        modBus.addListener(ModConfigs::onLoading);
        modBus.addListener(ModConfigs::onReloading);
        mariculture.domain.machine.ItemTags.install((item, tag) -> {
            var i = mariculture.platform.Stacks.item(item);
            return i != null && new net.minecraft.world.item.ItemStack(i).is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, new net.minecraft.resources.ResourceLocation(mariculture.platform.machine.MachineBlockEntity.forgeTag(tag))));
        });
        MinecraftForge.EVENT_BUS.addListener(ZombieGearEvents::onJoin);
        MinecraftForge.EVENT_BUS.addListener(mariculture.platform.event.SpawnGuardEvents::onPositionCheck);
        MinecraftForge.EVENT_BUS.addListener(mariculture.platform.event.SpawnGuardEvents::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(FisheryEvents::onAddReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(FisheryEvents::onTagsUpdated);
        MinecraftForge.EVENT_BUS.addListener(FisheryEvents::onItemFished);
        MinecraftForge.EVENT_BUS.addListener(FisheryEvents::onItemExpire);
        MinecraftForge.EVENT_BUS.addListener(FisheryEvents::onLivingDrops);
        MinecraftForge.EVENT_BUS.addListener(mariculture.platform.event.HammerEvents::onBreak);
        if (net.minecraftforge.fml.ModList.get().isLoaded("forestry")) {
            modBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) ->
                    e.enqueueWork(mariculture.platform.compat.forestry.ForestryCompat::fuels));
        }
        if (net.minecraftforge.fml.ModList.get().isLoaded("tconstruct")) {
            MinecraftForge.EVENT_BUS.addListener(mariculture.platform.compat.tconstruct.TinkersImport::onServerStarted);
            MinecraftForge.EVENT_BUS.addListener(mariculture.platform.compat.tconstruct.TinkersImport::onDatapackSync);
        }
        mariculture.platform.magic.JewelryEvents.register(modBus);
        mariculture.platform.magic.BlinkPacket.register();
        mariculture.platform.item.FluddPacket.register();
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.PlayerTickEvent e) -> {
            if (e.phase == net.minecraftforge.event.TickEvent.Phase.END) mariculture.platform.item.FluddPacket.tickFloat(e.player);
        });
        mariculture.platform.magic.MagicToolEvents.register();
        mariculture.platform.diving.DivingEvents.register();
        mariculture.platform.diving.ScubaEvents.register();
        MinecraftForge.EVENT_BUS.addListener(FisheryEvents::onRegisterCommands);
        if (FMLEnvironment.dist.isClient()) ModClient.register(modBus);
    }
}

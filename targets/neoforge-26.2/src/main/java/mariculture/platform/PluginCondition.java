package mariculture.platform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import mariculture.domain.content.Ids;
import mariculture.domain.plugin.Plugins;

/** The "mariculture:plugin" data condition: the plugin's mod is installed and its config switch is on. */
public record PluginCondition(String plugin) implements ICondition {
    public static final MapCodec<PluginCondition> CODEC = RecordCodecBuilder.mapCodec(b -> b.group(
            Codec.STRING.fieldOf("plugin").forGetter(PluginCondition::plugin)).apply(b, PluginCondition::new));
    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, Ids.MOD_ID);

    static {
        CONDITIONS.register("plugin", () -> CODEC);
    }

    public static void register(IEventBus modBus) {
        Plugins.setModLoaded(id -> ModList.get().isLoaded(id));
        Plugins.setItemExists(id -> Stacks.item(id) != null);
        CONDITIONS.register(modBus);
    }

    @Override
    public boolean test(IContext context) {
        Plugins.Plugin p = Plugins.byName(plugin);
        return p != null && Plugins.active(p);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}

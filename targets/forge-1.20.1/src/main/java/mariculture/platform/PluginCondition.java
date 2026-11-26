package mariculture.platform;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import net.minecraftforge.fml.ModList;

import mariculture.domain.content.Ids;
import mariculture.domain.plugin.Plugins;

/** The "mariculture:plugin" data condition: the plugin's mod is installed and its config switch is on. */
public record PluginCondition(String plugin) implements ICondition {
    private static final ResourceLocation ID = new ResourceLocation(Ids.MOD_ID, "plugin");

    public static final IConditionSerializer<PluginCondition> SERIALIZER = new IConditionSerializer<>() {
        @Override
        public void write(JsonObject json, PluginCondition value) {
            json.addProperty("plugin", value.plugin());
        }

        @Override
        public PluginCondition read(JsonObject json) {
            return new PluginCondition(GsonHelper.getAsString(json, "plugin"));
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    };

    public static void register() {
        Plugins.setModLoaded(id -> ModList.get().isLoaded(id));
        Plugins.setItemExists(id -> Stacks.item(id) != null);
        CraftingHelper.register(SERIALIZER);
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        Plugins.Plugin p = Plugins.byName(plugin);
        return p != null && Plugins.active(p);
    }
}

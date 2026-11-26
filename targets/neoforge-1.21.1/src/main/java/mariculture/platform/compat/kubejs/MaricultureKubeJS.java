package mariculture.platform.compat.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptManager;
import mariculture.domain.plugin.ScriptTweaks;

/**
 * KubeJS (listed in kubejs.plugins.txt, so only loaded with KubeJS): server scripts get {@code Mariculture}, which takes
 * the data-pack tweak format (core-domain ScriptTweaks, docs/tweaks.md). The last load's changes are cleared before
 * each server script load.
 */
public class MaricultureKubeJS implements KubeJSPlugin {
    @Override
    public void beforeScriptsLoaded(ScriptManager manager) {
        if (manager.scriptType.isServer()) ScriptTweaks.INSTANCE.reset();
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        if (bindings.type().isServer()) bindings.add("Mariculture", ScriptTweaks.INSTANCE);
    }
}

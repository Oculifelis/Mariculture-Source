package mariculture.platform.compat.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import mariculture.domain.plugin.ScriptTweaks;

/**
 * KubeJS (listed in kubejs.plugins.txt, so only loaded with KubeJS): server scripts get {@code Mariculture}, which takes
 * the data-pack tweak format (core-domain ScriptTweaks, docs/tweaks.md). Bindings are registered once per server
 * script load, so the last load's changes are cleared there first.
 */
public class MaricultureKubeJS extends KubeJSPlugin {
    @Override
    public void registerBindings(BindingsEvent event) {
        if (!event.getType().isServer()) return;
        ScriptTweaks.INSTANCE.reset();
        event.add("Mariculture", ScriptTweaks.INSTANCE);
    }
}

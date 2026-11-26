package mariculture.platform;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import mariculture.domain.config.Settings;

/** Builds the common config from the domain's {@link Settings} and pushes loaded values back into it. */
public final class ModConfigs {
    private static final List<Runnable> APPLY = new ArrayList<>();
    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        String section = null;
        for (Settings.Setting s : Settings.ALL) {
            if (!s.section().equals(section)) {
                if (section != null) b.pop();
                b.push(s.section());
                section = s.section();
            }
            b.comment(s.comment());
            if (s instanceof Settings.Bool x) {
                var v = b.define(x.key(), x.def());
                APPLY.add(() -> x.set(v.get()));
            } else if (s instanceof Settings.Int x) {
                var v = b.defineInRange(x.key(), x.def(), x.min(), x.max());
                APPLY.add(() -> x.set(v.get()));
            } else if (s instanceof Settings.Num x) {
                var v = b.defineInRange(x.key(), x.def(), 0.0, 1.0);
                APPLY.add(() -> x.set(v.get()));
            } else if (s instanceof Settings.Strs x) {
                var v = b.defineListAllowEmpty(x.key(), x.def(), o -> o instanceof String);
                APPLY.add(() -> x.set(v.get().stream().map(String::valueOf).toList()));
            }
        }
        if (section != null) b.pop();
        SPEC = b.build();
    }

    private ModConfigs() {}

    public static void onLoading(ModConfigEvent.Loading event) {
        apply(event);
    }

    public static void onReloading(ModConfigEvent.Reloading event) {
        apply(event);
    }

    private static void apply(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        APPLY.forEach(Runnable::run);
        mariculture.domain.env.BiomeEnvironment.rebuild(); // plugin switches gate data files
        mariculture.domain.fishing.RodTiers.rebuild();
    }
}

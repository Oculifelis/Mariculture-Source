package mariculture.domain.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import mariculture.domain.fishing.FishingRules;
import mariculture.domain.metal.Anvil;
import mariculture.domain.metal.Casting;
import mariculture.domain.metal.CrucibleFuels;
import mariculture.domain.metal.Melting;
import mariculture.domain.metal.VatRecipes;

/**
 * Script changes to the machine tables: the legacy MineTweaker handlers (plugins/minetweaker/Anvil.java,
 * Casting.java, Crucible.java, Fishing.java, Vat.java) are CraftTweaker classes now, and each script call becomes one
 * {@link Change} here. Every change can be taken back, so a reload that runs the scripts again starts clean
 * (legacy IUndoableAction.undo).
 *
 * <p>The tables ask this class first: additions win over the built-in rows, removals hide them. Ids are registry ids;
 * fluids are full fluid ids. KubeJS scripts ({@link ScriptTweaks}) and data packs ({@link TweakData}) make the same
 * changes, each replaced as a whole on every reload; CraftTweaker changes come first, then KubeJS, then data packs, so
 * the earlier one wins where two add the same thing.
 */
public final class Tweaks {
    /** One script change. */
    public sealed interface Change permits VatAdd, VatRemove, MeltAdd, MeltRemove, FuelAdd, FuelRemove, AnvilAdd, AnvilRemove,
            CastAdd, CastRemove, LootAdd, LootRemove {}

    public record VatAdd(VatRecipes.Recipe recipe) implements Change {}

    /** Vat.removeRecipe: every recipe with this output item and/or fluid (null = not checked). */
    public record VatRemove(String outItem, String outFluid) implements Change {}

    public record MeltAdd(Melting.Recipe recipe) implements Change {}

    /** Crucible.removeRecipe: the item no longer melts (fish oil included). */
    public record MeltRemove(String input) implements Change {}

    /** Crucible.addFuel: {@code key} is an item id, "#tag" or, when {@code fluid}, a fluid id or path. */
    public record FuelAdd(String key, boolean fluid, CrucibleFuels.Fuel fuel) implements Change {}

    public record FuelRemove(String key, boolean fluid) implements Change {}

    public record AnvilAdd(Anvil.Recipe recipe) implements Change {}

    public record AnvilRemove(String input) implements Change {}

    /** Casting.add*Recipe: {@code amount} mB of {@code fluid} cast {@code item} in that kind of caster. */
    public record CastAdd(Casting.Kind kind, String fluid, String item, int amount) implements Change {}

    /** Casting.remove*Recipe: that kind of caster no longer makes {@code item}. */
    public record CastRemove(Casting.Kind kind, String item) implements Change {}

    /** Fishing.addJunk/addGood/addRare; {@code exact} means only on {@code loot.minRod()} itself (Loot.exact). */
    public record LootAdd(FishingRules.Loot loot, boolean exact) implements Change {}

    /** Fishing.removeLoot: from every rarity and dimension. */
    public record LootRemove(String item) implements Change {}

    private static final List<Change> CHANGES = new ArrayList<>();
    private static final List<Change> IMPORTS = new ArrayList<>();
    private static final List<Change> DATA = new ArrayList<>();
    private static final List<Change> SCRIPT = new ArrayList<>();

    private Tweaks() {}

    public static synchronized void apply(Change c) {
        CHANGES.add(Objects.requireNonNull(c));
    }

    public static synchronized void undo(Change c) {
        CHANGES.remove(c);
    }

    public static synchronized void clear() {
        CHANGES.clear();
        IMPORTS.clear();
        DATA.clear();
        SCRIPT.clear();
    }

    /** A KubeJS script change; {@link #clearScript} runs before each server script load. */
    public static synchronized void addScript(Change c) {
        SCRIPT.add(Objects.requireNonNull(c));
    }

    public static synchronized void clearScript() {
        SCRIPT.clear();
    }

    /** The data packs' changes ({@code mariculture/tweaks}), replaced on every data reload. */
    public static synchronized void setData(Collection<? extends Change> data) {
        DATA.clear();
        DATA.addAll(data);
    }

    /** For /mariculture info: how many changes each source has made. */
    public static synchronized String summary() {
        return "tweaks: " + CHANGES.size() + " CraftTweaker, " + SCRIPT.size() + " KubeJS, " + DATA.size() + " data, " + IMPORTS.size() + " imported";
    }

    public static synchronized List<Change> changes() {
        return List.copyOf(CHANGES);
    }

    /**
     * Recipes imported from another mod's machines (Tinkers' smeltery, PluginTConstruct.java:95-127): melting and
     * casting rows used only after the built-in tables, replaced as a whole whenever that mod's recipes reload.
     */
    public static synchronized void setImports(Collection<? extends Change> imports) {
        IMPORTS.clear();
        IMPORTS.addAll(imports);
    }

    private static synchronized <T extends Change> List<T> of(Class<T> type) {
        List<T> out = filter(CHANGES, type);
        out.addAll(filter(SCRIPT, type));
        out.addAll(filter(DATA, type));
        return out;
    }

    private static synchronized <T extends Change> List<T> imported(Class<T> type) {
        return filter(IMPORTS, type);
    }

    private static <T extends Change> List<T> filter(List<Change> from, Class<T> type) {
        List<T> out = new ArrayList<>();
        for (Change c : from) if (type.isInstance(c)) out.add(type.cast(c));
        return out;
    }

    // Vat

    /** The script's vat recipes, then the built-in ones that were not removed. */
    public static List<VatRecipes.Recipe> vat(Collection<VatRecipes.Recipe> builtIn) {
        List<VatRecipes.Recipe> out = new ArrayList<>();
        for (VatAdd a : of(VatAdd.class)) out.add(a.recipe());
        List<VatRemove> removed = of(VatRemove.class);
        for (VatRecipes.Recipe r : builtIn) if (removed.stream().noneMatch(x -> removes(x, r))) out.add(r);
        return out;
    }

    static boolean removes(VatRemove x, VatRecipes.Recipe r) {
        boolean item = x.outItem() == null || (r.outItem() != null && r.outItem().is(x.outItem()));
        boolean fluid = x.outFluid() == null || (r.outFluid() != null && r.outFluid().fluid().equals(x.outFluid()));
        return item && fluid;
    }

    // Crucible

    public static List<Melting.Recipe> melting(Collection<Melting.Recipe> builtIn) {
        List<Melting.Recipe> out = new ArrayList<>();
        for (MeltAdd a : of(MeltAdd.class)) out.add(a.recipe());
        for (Melting.Recipe r : builtIn) if (!meltRemoved(r.input())) out.add(r);
        for (MeltAdd a : imported(MeltAdd.class)) if (!meltRemoved(a.recipe().input())) out.add(a.recipe());
        return out;
    }

    public static boolean meltRemoved(String input) {
        return of(MeltRemove.class).stream().anyMatch(x -> x.input().equals(input));
    }

    /** A script fuel for the item, or {@code builtIn}; null when the script removed it. */
    public static CrucibleFuels.Fuel itemFuel(String item, java.util.function.Predicate<String> hasTag,
                                              java.util.function.Supplier<CrucibleFuels.Fuel> builtIn) {
        for (FuelAdd a : of(FuelAdd.class)) {
            if (!a.fluid() && (a.key().startsWith("#") ? hasTag.test(a.key().substring(1)) : a.key().equals(item))) return a.fuel();
        }
        if (of(FuelRemove.class).stream().anyMatch(x -> !x.fluid() && (x.key().startsWith("#") ? hasTag.test(x.key().substring(1)) : x.key().equals(item)))) {
            return null;
        }
        return builtIn.get();
    }

    /** Fluids match by full id or by path (legacy fuels were fluid names). */
    public static CrucibleFuels.Fuel fluidFuel(String fluid, java.util.function.Supplier<CrucibleFuels.Fuel> builtIn) {
        String path = fluid.substring(fluid.indexOf(':') + 1);
        for (FuelAdd a : of(FuelAdd.class)) {
            if (a.fluid() && (a.key().equals(fluid) || a.key().equals(path))) return a.fuel();
        }
        if (of(FuelRemove.class).stream().anyMatch(x -> x.fluid() && (x.key().equals(fluid) || x.key().equals(path)))) return null;
        return builtIn.get();
    }

    // Anvil

    public static Anvil.Recipe anvil(String item, java.util.function.Supplier<Anvil.Recipe> builtIn) {
        for (AnvilAdd a : of(AnvilAdd.class)) if (a.recipe().input().equals(item)) return a.recipe();
        if (of(AnvilRemove.class).stream().anyMatch(x -> x.input().equals(item))) return null;
        return builtIn.get();
    }

    public static List<Anvil.Recipe> anvil(Collection<Anvil.Recipe> builtIn) {
        List<Anvil.Recipe> out = new ArrayList<>();
        for (AnvilAdd a : of(AnvilAdd.class)) out.add(a.recipe());
        for (Anvil.Recipe r : builtIn) {
            String in = r.input();
            if (out.stream().noneMatch(a -> a.input().equals(in)) && of(AnvilRemove.class).stream().noneMatch(x -> x.input().equals(in))) {
                out.add(r);
            }
        }
        return out;
    }

    // Casting

    public static Casting.Result cast(Casting.Kind kind, String fluid, java.util.function.Supplier<Casting.Result> builtIn) {
        for (CastAdd a : of(CastAdd.class)) if (a.kind() == kind && a.fluid().equals(fluid)) return new Casting.Result(a.item(), a.amount());
        Casting.Result r = builtIn.get();
        if (r == null) {
            for (CastAdd a : imported(CastAdd.class)) if (a.kind() == kind && a.fluid().equals(fluid)) r = new Casting.Result(a.item(), a.amount());
        }
        Casting.Result found = r;
        if (found != null && of(CastRemove.class).stream().anyMatch(x -> x.kind() == kind && x.item().equals(found.item()))) return null;
        return found;
    }

    public static List<CastAdd> casts() {
        return of(CastAdd.class);
    }

    // Fishing

    /** The loot pool for a rarity on this rod: script loot first, then the built-in pool minus removed items. */
    public static List<FishingRules.Loot> loot(FishingRules.Rarity rarity, mariculture.domain.fish.Species.Rod rod, List<FishingRules.Loot> builtIn) {
        List<FishingRules.Loot> out = new ArrayList<>();
        for (LootAdd a : of(LootAdd.class)) {
            if (a.loot().rarity() != rarity || (a.exact() && a.loot().minRod() != rod)) continue;
            out.add(a.loot());
        }
        List<LootRemove> removed = of(LootRemove.class);
        for (FishingRules.Loot l : builtIn) if (removed.stream().noneMatch(x -> x.item().equals(l.item()))) out.add(l);
        return out;
    }
}

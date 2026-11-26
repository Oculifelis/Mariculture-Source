package mariculture.platform.compat.hephaestus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.titammods.setup.ModRecipes;

import mariculture.domain.metal.Casting;
import mariculture.domain.metal.Melting;
import mariculture.domain.plugin.Plugins;
import mariculture.domain.plugin.Tweaks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The Tinkers import (PluginTConstruct.java:95-127, config TCON_RECIPES) on Hephaestus: every smeltery melting becomes
 * a crucible melting at a scaled temperature, every table casting on Hephaestus' ingot or nugget cast an ingot or
 * nugget caster recipe, and every basin casting without a cast a block caster recipe. Imports come after Mariculture's
 * own tables (Tweaks.setImports) and are rebuilt whenever the server's recipes load. Only registered while Hephaestus
 * is loaded.
 */
public final class HephaestusImport {
    private HephaestusImport() {}

    public static void onServerStarted(ServerStartedEvent event) {
        run(event.getServer().getRecipeManager());
    }

    /** A /reload syncs datapacks with no player once the new recipes are in. */
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) run(event.getPlayerList().getServer().getRecipeManager());
    }

    static void run(RecipeManager recipes) {
        List<Tweaks.Change> out = new ArrayList<>();
        if (Plugins.hephaestusImport()) {
            for (RecipeHolder<ModRecipes.MeltingRecipe> h : recipes.recipeMap().byType(ModRecipes.MELTING_TYPE.get())) melting(h.value(), out);
            for (RecipeHolder<ModRecipes.CastingTableRecipe> h : recipes.recipeMap().byType(ModRecipes.CASTING_TABLE_TYPE.get())) {
                ModRecipes.CastingTableRecipe r = h.value();
                Casting.Kind kind = castKind(r.cast());
                if (kind != null) casting(kind, r.fluidStack(), r.result(), out);
            }
            for (RecipeHolder<ModRecipes.CastingBasinRecipe> h : recipes.recipeMap().byType(ModRecipes.CASTING_BASIN_TYPE.get())) {
                ModRecipes.CastingBasinRecipe r = h.value();
                if (!r.hasCast()) casting(Casting.Kind.BLOCK, r.fluidStack(), r.result(), out);
            }
        }
        Tweaks.setImports(out);
    }

    private static void melting(ModRecipes.MeltingRecipe r, List<Tweaks.Change> out) {
        FluidStack fluid = r.output();
        if (fluid.isEmpty()) return;
        String fluidId = BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString();
        int temperature = Plugins.tinkersToCrucible(r.temperature());
        r.input().items().forEach(item -> out.add(new Tweaks.MeltAdd(new Melting.Recipe(BuiltInRegistries.ITEM.getKey(item.value()).toString(),
                temperature, fluidId, fluid.getAmount(), null, 0, List.of(), List.of()))));
    }

    private static Casting.Kind castKind(Optional<Ingredient> cast) {
        if (cast.isEmpty()) return null;
        return cast.get().items().map(item -> BuiltInRegistries.ITEM.getKey(item.value()).toString())
                .map(id -> id.equals("hephaestus:ingot_cast") ? Casting.Kind.INGOT : id.equals("hephaestus:nugget_cast") ? Casting.Kind.NUGGET : null)
                .filter(k -> k != null).findFirst().orElse(null);
    }

    private static void casting(Casting.Kind kind, FluidStack fluid, ItemStack result, List<Tweaks.Change> out) {
        if (fluid.isEmpty() || result.isEmpty()) return;
        out.add(new Tweaks.CastAdd(kind, BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString(),
                BuiltInRegistries.ITEM.getKey(result.getItem()).toString(), fluid.getAmount()));
    }
}

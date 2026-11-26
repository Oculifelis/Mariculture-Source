package mariculture.platform.compat.tconstruct;

import java.util.ArrayList;
import java.util.List;

import mariculture.domain.metal.Casting;
import mariculture.domain.metal.Melting;
import mariculture.domain.plugin.Plugins;
import mariculture.domain.plugin.Tweaks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.ItemCastingRecipe;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;

/**
 * The experimental Tinkers import (PluginTConstruct.java:95-127, config TCON_RECIPES): every smeltery melting becomes a
 * crucible melting at a scaled temperature, every table casting on an ingot or nugget cast an ingot or nugget caster
 * recipe, and every basin casting without a cast a block caster recipe. Imports come after Mariculture's own tables
 * (Tweaks.setImports) and are rebuilt whenever the server's recipes load. Only registered while Tinkers is loaded.
 *
 * <p>Change: legacy only imported basin recipes that had a cast, which left out every metal block.
 */
public final class TinkersImport {
    private static final TagKey<Item> INGOT_CASTS = cast("multi_use/ingot"), INGOT_SAND = cast("single_use/ingot"),
            NUGGET_CASTS = cast("multi_use/nugget"), NUGGET_SAND = cast("single_use/nugget");

    private TinkersImport() {}

    private static TagKey<Item> cast(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("tconstruct", "casts/" + path));
    }

    public static void onServerStarted(ServerStartedEvent event) {
        run(event.getServer().getRecipeManager());
    }

    /** A /reload syncs datapacks with no player once the new recipes are in. */
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) run(event.getPlayerList().getServer().getRecipeManager());
    }

    static void run(RecipeManager recipes) {
        List<Tweaks.Change> out = new ArrayList<>();
        if (Plugins.tinkersImport()) {
            for (IMeltingRecipe r : recipes.getAllRecipesFor(TinkerRecipeTypes.MELTING.get())) melting(r, out);
            for (ICastingRecipe r : recipes.getAllRecipesFor(TinkerRecipeTypes.CASTING_TABLE.get())) casting(r, false, out);
            for (ICastingRecipe r : recipes.getAllRecipesFor(TinkerRecipeTypes.CASTING_BASIN.get())) casting(r, true, out);
        }
        Tweaks.setImports(out);
    }

    private static void melting(IMeltingRecipe recipe, List<Tweaks.Change> out) {
        if (!(recipe instanceof MeltingRecipe r)) return;
        FluidStack fluid = r.getOutput();
        if (fluid.isEmpty()) return;
        String fluidId = BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString();
        int temperature = Plugins.tinkersToCrucible(r.getTemperature());
        for (ItemStack in : r.getInput().getItems()) {
            out.add(new Tweaks.MeltAdd(new Melting.Recipe(BuiltInRegistries.ITEM.getKey(in.getItem()).toString(), temperature, fluidId,
                    fluid.getAmount(), null, 0, List.of(), List.of())));
        }
    }

    private static void casting(ICastingRecipe recipe, boolean basin, List<Tweaks.Change> out) {
        if (!(recipe instanceof ItemCastingRecipe r)) return;
        Casting.Kind kind;
        if (basin) {
            if (!r.getCast().isEmpty()) return;
            kind = Casting.Kind.BLOCK;
        } else {
            kind = null;
            for (ItemStack cast : r.getCast().getItems()) {
                if (cast.is(INGOT_CASTS) || cast.is(INGOT_SAND)) kind = Casting.Kind.INGOT;
                else if (cast.is(NUGGET_CASTS) || cast.is(NUGGET_SAND)) kind = Casting.Kind.NUGGET;
            }
            if (kind == null) return;
        }
        ItemStack result = r.getOutput();
        if (result.isEmpty()) return;
        String item = BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
        for (FluidStack fluid : r.getFluids()) {
            if (fluid.isEmpty()) continue;
            out.add(new Tweaks.CastAdd(kind, BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString(), item, fluid.getAmount()));
        }
    }
}

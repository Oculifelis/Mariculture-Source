package mariculture.platform.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

import mariculture.domain.fishery.FishRecipes;
import mariculture.domain.machine.Stack;
import mariculture.platform.Stacks;

/** Crafting recipes whose result comes from core-domain FishRecipes (caviar from eggs, fish DNA reset). */
public final class FishCraftingRecipe extends CustomRecipe {
    public static final RecipeSerializer<FishCraftingRecipe> CAVIAR = new SimpleCraftingRecipeSerializer<>(c -> new FishCraftingRecipe(c, true));
    public static final RecipeSerializer<FishCraftingRecipe> FISH_RESET = new SimpleCraftingRecipeSerializer<>(c -> new FishCraftingRecipe(c, false));

    private final boolean caviar;

    private FishCraftingRecipe(CraftingBookCategory category, boolean caviar) {
        super(category);
        this.caviar = caviar;
    }

    private ItemStack result(CraftingInput grid) {
        List<Stack> stacks = new ArrayList<>();
        for (int i = 0; i < grid.size(); i++) stacks.add(Stacks.toDomain(grid.getItem(i)));
        Function<List<Stack>, Stack> rule = caviar ? FishRecipes::caviar : FishRecipes::reset;
        return Stacks.toLoader(rule.apply(stacks));
    }

    @Override
    public boolean matches(CraftingInput grid, Level level) {
        return !result(grid).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput grid, HolderLookup.Provider registries) {
        return result(grid);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return caviar ? CAVIAR : FISH_RESET;
    }
}

package mariculture.domain.fishery;

import java.util.List;

import mariculture.domain.fish.Breeding;
import mariculture.domain.fish.Egg;
import mariculture.domain.fish.Genome;
import mariculture.domain.fish.LiveFish;
import mariculture.domain.fish.Species;
import mariculture.domain.machine.Stack;

/**
 * The two crafting recipes with code behind them (fishery/ShapelessFishRecipe, ShapelessResetFishRecipe). Each takes
 * the crafting grid and returns the result, or EMPTY when the grid does not match.
 */
public final class FishRecipes {
    public static final String CAVIAR = "mariculture:caviar";

    private FishRecipes() {}

    /** The one non-empty stack of the grid, or null when there are none or several. */
    private static Stack single(List<Stack> grid) {
        Stack found = null;
        for (Stack s : grid) {
            if (s.isEmpty()) continue;
            if (found != null) return null;
            found = s;
        }
        return found;
    }

    /**
     * Fish eggs alone make caviar: max(1, ceil(eggs left / 650)), at most 64. BUG-039: legacy divided integers
     * before the ceiling (so it truncated) and stored the count in the shared recipe output.
     */
    public static Stack caviar(List<Stack> grid) {
        Stack s = single(grid);
        if (s == null || !s.is(LiveFish.EGGS)) return Stack.EMPTY;
        Egg egg = LiveFish.egg(s);
        int eggs = egg == null ? 0 : egg.fertility();
        return Stack.of(CAVIAR, Math.min(64, Math.max(1, (int) Math.ceil(eggs / 650.0))));
    }

    /** A pure live fish alone resets to its species' wild genome, keeping its sex (Breeding.resetToSpecies). */
    public static Stack reset(List<Stack> grid) {
        Stack s = single(grid);
        if (s == null || !LiveFish.isLiveFish(s)) return Stack.EMPTY;
        Genome g = LiveFish.genome(s);
        Species sp = g == null ? null : LiveFish.registry().species(g.species());
        if (sp == null) return Stack.EMPTY;
        Genome reset = Breeding.resetToSpecies(g, sp);
        return reset == null ? Stack.EMPTY : LiveFish.stack(reset);
    }
}

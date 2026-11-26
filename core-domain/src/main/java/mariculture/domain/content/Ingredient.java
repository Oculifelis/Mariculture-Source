package mariculture.domain.content;

/**
 * A recipe ingredient: one item or an item tag. Tags are written in a target-neutral form and
 * resolved by {@link TagNames} ("c:" style common tags become "forge:" on Forge 1.20.1).
 */
public record Ingredient(String value, boolean tag) {
    public static Ingredient item(String id) {
        return new Ingredient(Ids.full(id), false);
    }

    public static Ingredient tag(String tag) {
        return new Ingredient(tag, true);
    }
}

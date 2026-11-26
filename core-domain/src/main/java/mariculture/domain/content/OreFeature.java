package mariculture.domain.content;

/**
 * An ore vein feature. Legacy IWorldGenerator settings map as: veins per chunk -> count,
 * vein size -> ore size, Y range -> uniform height with an inclusive top of {@code max - 1}
 * (legacy rolled {@code min + nextInt(max - min)}).
 *
 * {@code oneInChunks} above 1 is a rarity filter: one attempt per that many chunks (legacy nextInt(chance) == 0).
 *
 * <p>Legacy configs disabled generation with a boolean; modern packs disable or retune a vein by
 * overriding the placed feature or biome modifier JSON in a datapack (docs/behavior_specs/worldgen.md).
 */
public record OreFeature(String id, String block, int size, int veinsPerChunk, int minY, int maxYExclusive,
                         String replaceableTag, String biomeTag, String legacy, int oneInChunks) {
    public OreFeature(String id, String block, int size, int veinsPerChunk, int minY, int maxYExclusive,
                      String replaceableTag, String biomeTag, String legacy) {
        this(id, block, size, veinsPerChunk, minY, maxYExclusive, replaceableTag, biomeTag, legacy, 1);
    }

    public OreFeature {
        if (oneInChunks < 1) throw new IllegalArgumentException(id + ": one-in-chunks must be positive");
        Ids.requireValid(id);
        Ids.requireValid(block);
        if (size < 1 || size > 64) throw new IllegalArgumentException(id + ": ore size must be 1-64, got " + size);
        if (veinsPerChunk < 1) throw new IllegalArgumentException(id + ": veins per chunk must be positive");
        // The legacy generator crashed (nextInt of a non-positive bound) when max <= min.
        if (maxYExclusive <= minY) throw new IllegalArgumentException(id + ": maximum Y must be above minimum Y");
    }
}

package mariculture.platform.worldgen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.resources.ResourceLocation;

import mariculture.domain.content.Ids;
import mariculture.domain.random.Rng;
import mariculture.domain.worldgen.Seabed;
import mariculture.domain.worldgen.Seabed.Action;
import mariculture.domain.worldgen.Seabed.Cell;
import mariculture.domain.worldgen.Seabed.Kind;
import mariculture.platform.ModContent;

/**
 * Lays the legacy limestone sea bed (with rutile) under oceans, rivers and beaches. Placed once per chunk in the
 * raw_generation step (before ores, plants and oysters) with no placement modifiers, so it only ever touches its own chunk's 16x16 columns.
 * The column rules live in {@link Seabed}; this class only reads and writes blocks.
 */
public final class SeabedFeature extends Feature<NoneFeatureConfiguration> {
    /** Biomes that get a sea bed; a data pack can change this tag. */
    public static final TagKey<Biome> HAS_SEABED = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Ids.MOD_ID, "has_limestone_seabed"));
    /** Loose floor blocks the bed lies under; a data pack can change this tag. */
    public static final TagKey<Block> FLOOR = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Ids.MOD_ID, "seabed_floor"));
    private static final double NOISE_SCALE = 0.0625;
    private static final int MAX_CELLS = Seabed.MAX_SURFACE + 40;

    private record SeededNoise(long seed, PerlinSimplexNoise noise) {}

    private volatile SeededNoise noise;

    public SeabedFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        PerlinSimplexNoise surfaceNoise = noise(level.getSeed());
        Rng rng = rng(ctx.random());
        ChunkPos chunk = new ChunkPos(ctx.origin());
        int seaLevel = ctx.chunkGenerator().getSeaLevel();
        BlockState limestone = ModContent.block("limestone").defaultBlockState();
        BlockState rutile = ModContent.block("rutile_ore").defaultBlockState();
        BlockState sand = Blocks.SAND.defaultBlockState();
        boolean changed = false;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunk.getMinBlockX() + dx;
                int z = chunk.getMinBlockZ() + dz;
                int floorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                Holder<Biome> biome = level.getBiome(pos.set(x, floorY, z));
                if (!biome.is(HAS_SEABED)) continue;

                List<Cell> cells = new ArrayList<>();
                for (int y = floorY; y > level.getMinBuildHeight() && cells.size() < MAX_CELLS; y--) {
                    cells.add(classify(level.getBlockState(pos.set(x, y, z))));
                }
                double n = surfaceNoise.getValue(x * NOISE_SCALE, z * NOISE_SCALE, false);
                Action[] plan = Seabed.plan(Seabed.Settings.LEGACY, kind(biome), cells.toArray(new Cell[0]), floorY,
                        seaLevel, n, rng);
                for (int i = 0; i < plan.length; i++) {
                    BlockState state = switch (plan[i]) {
                        case KEEP -> null;
                        case SAND -> sand;
                        case LIMESTONE -> limestone;
                        case RUTILE -> rutile;
                    };
                    if (state == null) continue;
                    level.setBlock(pos.set(x, floorY - i, z), state, 2);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static Cell classify(BlockState state) {
        if (state.is(BlockTags.STONE_ORE_REPLACEABLES)) return Cell.STONE;
        if (state.is(FLOOR)) return Cell.SURFACE;
        return Cell.OTHER;
    }

    private static Kind kind(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_DEEP_OCEAN)) return Kind.DEEP_OCEAN;
        if (biome.is(BiomeTags.IS_RIVER)) return Kind.RIVER;
        if (biome.is(BiomeTags.IS_BEACH)) return Kind.BEACH;
        return Kind.OCEAN;
    }

    private PerlinSimplexNoise noise(long seed) {
        SeededNoise current = noise;
        if (current == null || current.seed() != seed) {
            current = new SeededNoise(seed,
                    new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(seed)), List.of(-3, -2, -1, 0)));
            noise = current;
        }
        return current.noise();
    }

    static Rng rng(RandomSource random) {
        return new Rng() {
            @Override
            public float nextFloat() {
                return random.nextFloat();
            }

            @Override
            public int nextInt(int bound) {
                return random.nextInt(bound);
            }
        };
    }
}

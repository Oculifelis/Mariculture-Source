package mariculture.platform.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import mariculture.domain.content.Ids;
import mariculture.domain.random.Rng;
import mariculture.domain.worldgen.Coral;
import mariculture.domain.worldgen.OceanDecor;
import mariculture.platform.ModContent;
import mariculture.platform.block.CoralPlantBlock;

/**
 * Coral reefs, kelp forests with ocean chests and ancient sand on saline sea floors (domain OceanDecor), once per
 * chunk and inside that chunk only.
 */
public final class OceanDecorFeature extends Feature<NoneFeatureConfiguration> {
    public static final TagKey<Biome> SALINE = TagKey.create(Registries.BIOME, new ResourceLocation(Ids.MOD_ID, "is_saline"));
    public static final TagKey<Block> LIMESTONE = TagKey.create(Registries.BLOCK, new ResourceLocation(Ids.MOD_ID, "limestone"));
    public static final ResourceLocation OCEAN_CHEST = new ResourceLocation(Ids.MOD_ID, "chests/ocean_chest");

    public OceanDecorFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ChunkPos chunk = new ChunkPos(ctx.origin());
        int cx = chunk.getMinBlockX(), cz = chunk.getMinBlockZ();
        if (!level.getBiome(new BlockPos(cx + 8, level.getSeaLevel(), cz + 8)).is(SALINE)) return false;
        OceanDecor.Settings s = OceanDecor.Settings.LEGACY;
        Rng rng = SeabedFeature.rng(ctx.random());
        boolean placed = false;
        if (s.ancientSand() && rng.nextInt(Math.max(1, s.ancientSandOneIn())) == 0) {
            placed |= ancientSand(level, s, rng, floor(level, cx + 4 + rng.nextInt(8), cz + 4 + rng.nextInt(8)));
        }
        if (s.reefs() && OceanDecor.active(regionRoll(level, chunk, 0x5EEF), s.reefStart(), s.reefEnd())) {
            for (int i = 0; i < OceanDecor.REEF_ATTEMPTS; i++) {
                placed |= reef(level, rng, floor(level, cx + 4 + rng.nextInt(8), cz + 4 + rng.nextInt(8)));
            }
        }
        if (s.forests() && OceanDecor.active(regionRoll(level, chunk, 0x4E1B), s.forestStart(), s.forestEnd())) {
            for (int i = 0; i < OceanDecor.FOREST_ATTEMPTS; i++) {
                placed |= kelp(level, s, rng, floor(level, cx + 8 + rng.nextInt(8) - rng.nextInt(8), cz + 8 + rng.nextInt(8) - rng.nextInt(8)));
            }
        }
        return placed;
    }

    /** The first water block above the sea floor. */
    private static BlockPos floor(WorldGenLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z), z);
    }

    /** A number in [0, 1) for this 4x4-chunk region, the same for every chunk in it. */
    private static double regionRoll(WorldGenLevel level, ChunkPos chunk, long salt) {
        long rx = Math.floorDiv(chunk.getMinBlockX() >> 4, OceanDecor.REGION), rz = Math.floorDiv(chunk.getMinBlockZ() >> 4, OceanDecor.REGION);
        return new java.util.Random(level.getSeed() ^ rx * 341873128712L ^ rz * 132897987541L ^ salt).nextDouble();
    }

    private static boolean water(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.WATER);
    }

    /** WorldGenAncientSand: dirt, grass, sand and gravel within the radius and two blocks up and down turn to ancient sand. */
    private static boolean ancientSand(WorldGenLevel level, OceanDecor.Settings s, Rng rng, BlockPos at) {
        if (!water(level, at)) return false;
        BlockState sand = ModContent.block("ancient_sand").defaultBlockState();
        int r = OceanDecor.ancientSandRadius(s, rng), h = OceanDecor.ANCIENT_SAND_HALF_HEIGHT;
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (dx * dx + dz * dz > r * r) continue;
            for (int dy = -h; dy <= h; dy++) {
                BlockPos p = at.offset(dx, dy, dz);
                BlockState b = level.getBlockState(p);
                if (b.is(Blocks.DIRT) || b.is(Blocks.GRASS_BLOCK) || b.is(Blocks.SAND) || b.is(Blocks.GRAVEL)) level.setBlock(p, sand, 2);
            }
        }
        return true;
    }

    /** WorldGenReef: sea floor under two blocks of water turns to coral rock in a disc, topped with dried coral or a wet sponge. */
    private static boolean reef(WorldGenLevel level, Rng rng, BlockPos at) {
        BlockState rock = ModContent.block("coral_rock").defaultBlockState();
        int r = OceanDecor.reefRadius(rng);
        boolean placed = false;
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (dx * dx + dz * dz > r * r) continue;
            BlockPos top = at.offset(dx, 0, dz);
            BlockPos ground = top.below();
            BlockState g = level.getBlockState(ground);
            boolean replaceable = g.is(Blocks.SAND) || g.is(Blocks.GRAVEL) || g.is(Blocks.DIRT) || g.is(LIMESTONE);
            if (!replaceable || !water(level, top) || !water(level, top.above())) continue;
            level.setBlock(ground, rock, 2);
            BlockState plant = Coral.reefSponge(rng) ? Blocks.WET_SPONGE.defaultBlockState()
                    : ModContent.block(Coral.reefColour(rng).dried()).defaultBlockState().setValue(CoralPlantBlock.WATERLOGGED, true);
            level.setBlock(top, plant, 2);
            placed = true;
        }
        return placed;
    }

    /** WorldGenKelp (forests only): a vanilla kelp column, or on sand one time in chestOneIn an ocean chest. */
    private static boolean kelp(WorldGenLevel level, OceanDecor.Settings s, Rng rng, BlockPos at) {
        if (!water(level, at) || !water(level, at.above())) return false;
        BlockState ground = level.getBlockState(at.below());
        if (OceanDecor.chest(s, rng)) {
            if (!ground.is(Blocks.SAND)) return false;
            BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, net.minecraft.core.Direction.from2DDataValue(rng.nextInt(4)))
                    .setValue(ChestBlock.WATERLOGGED, true);
            level.setBlock(at, chest, 2);
            net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity.setLootTable(level, level.getRandom(), at, OCEAN_CHEST);
            return true;
        }
        if (!ground.isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP) || ground.is(BlockTags.ICE)) return false;
        int height = OceanDecor.forestKelpHeight(rng);
        BlockPos p = at;
        for (int i = 0; i < height && water(level, p) && water(level, p.above()); i++, p = p.above()) {
            boolean last = i == height - 1 || !water(level, p.above(2));
            level.setBlock(p, last ? Blocks.KELP.defaultBlockState().setValue(KelpBlock.AGE, 25) : Blocks.KELP_PLANT.defaultBlockState(), 2);
            if (last) break;
        }
        return true;
    }
}

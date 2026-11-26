package mariculture.platform.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import mariculture.domain.content.Ids;
import mariculture.domain.pearl.Oyster;
import mariculture.platform.ModContent;
import mariculture.platform.block.OysterBlock;
import mariculture.platform.block.OysterBlockEntity;

/** Pearl oysters on saline sea floors (WorldGenHandler.generateOyster), once per chunk, inside that chunk only. */
public final class OysterFeature extends Feature<NoneFeatureConfiguration> {
    public static final TagKey<Biome> HAS_OYSTERS = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Ids.MOD_ID, "has_oysters"));

    public OysterFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ChunkPos chunk = new ChunkPos(ctx.origin());
        BlockState oyster = ModContent.block("oyster").defaultBlockState();
        boolean placed = false;
        for (Oyster.Placement p : Oyster.generate(Oyster.Settings.LEGACY, SeabedFeature.rng(ctx.random()))) {
            int x = chunk.getMinBlockX() + p.dx();
            int z = chunk.getMinBlockZ() + p.dz();
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z), z);
            if (!level.getBiome(pos).is(HAS_OYSTERS) || !level.getBlockState(pos).is(Blocks.WATER)) continue;
            BlockState state = oyster.setValue(OysterBlock.FACING, Direction.from2DDataValue(p.facing()))
                    .setValue(OysterBlock.WATERLOGGED, true);
            if (!state.canSurvive(level, pos)) continue;
            level.setBlock(pos, state, 2);
            if (p.contents() != null && level.getBlockEntity(pos) instanceof OysterBlockEntity entity) {
                entity.setContents(p.contents());
            }
            placed = true;
        }
        return placed;
    }
}

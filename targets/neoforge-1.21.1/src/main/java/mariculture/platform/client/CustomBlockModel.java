package mariculture.platform.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

import mariculture.domain.content.Face;
import mariculture.domain.factory.CustomBlocks;
import mariculture.domain.factory.CustomFaces;
import mariculture.platform.factory.CustomBlockEntity;

/**
 * Draws a sawmill block (legacy RenderCustom*): its shape's model with every face swapped for the face of the block
 * it was cut from, and see-through faces left out. Each quad keeps its place and takes the source face's texture,
 * and is drawn in the source block's render layers.
 */
public final class CustomBlockModel extends BakedModelWrapper<BakedModel> {
    private record Source(TextureAtlasSprite sprite, ChunkRenderTypeSet layers) {}

    /** The source face for (block, side), cleared on every model reload. */
    private static final Map<String, Source> SOURCES = new ConcurrentHashMap<>();

    private CustomBlockModel(BakedModel original) {
        super(original);
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((ModelEvent.ModifyBakingResult e) -> {
            SOURCES.clear();
            for (Block b : BuiltInRegistries.BLOCK) {
                if (!CustomBlocks.isCustom(BuiltInRegistries.BLOCK.getKey(b).toString())) continue;
                for (BlockState st : b.getStateDefinition().getPossibleStates()) {
                    e.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(st), (k, m) -> new CustomBlockModel(m));
                }
            }
        });
    }

    private static Source source(String block, Face side) {
        Block b = CustomBlockEntity.faceBlock(block);
        if (b == null) return null;
        return SOURCES.computeIfAbsent(block + "|" + side, k -> {
            BlockState st = b.defaultBlockState();
            Direction d = Direction.from3DDataValue(side.ordinal());
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(st);
            RandomSource rand = RandomSource.create(42L);
            TextureAtlasSprite sprite = null;
            List<BakedQuad> quads = model.getQuads(st, d, rand, ModelData.EMPTY, null);
            if (!quads.isEmpty()) sprite = quads.get(0).getSprite();
            if (sprite == null) {
                for (BakedQuad q : model.getQuads(st, null, rand, ModelData.EMPTY, null)) {
                    if (q.getDirection() == d) {
                        sprite = q.getSprite();
                        break;
                    }
                }
            }
            if (sprite == null) sprite = model.getParticleIcon(ModelData.EMPTY);
            return new Source(sprite, model.getRenderTypes(st, rand, ModelData.EMPTY));
        });
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType layer) {
        CustomFaces faces = data.get(CustomBlockEntity.FACES);
        if (faces == null) return originalModel.getQuads(state, side, rand, data, layer);
        List<BakedQuad> out = new ArrayList<>();
        for (BakedQuad q : originalModel.getQuads(state, side, rand, ModelData.EMPTY, null)) {
            Face f = Face.values()[q.getDirection().get3DDataValue()];
            if (faces.block(f).equals(CustomFaces.AIR)) continue;
            Source src = source(faces.block(f), faces.side(f));
            if (src == null) {
                if (layer == null || layer == RenderType.solid()) out.add(q);
                continue;
            }
            if (layer != null && !src.layers().contains(layer)) continue;
            out.add(remap(q, src.sprite()));
        }
        return out;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        CustomFaces faces = data.get(CustomBlockEntity.FACES);
        if (faces == null) return super.getRenderTypes(state, rand, data);
        List<ChunkRenderTypeSet> sets = new ArrayList<>();
        sets.add(ChunkRenderTypeSet.of(RenderType.solid()));
        for (Face f : Face.values()) {
            Source src = source(faces.block(f), faces.side(f));
            if (src != null) sets.add(src.layers());
        }
        return ChunkRenderTypeSet.union(sets);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        CustomFaces faces = data.get(CustomBlockEntity.FACES);
        Source src = faces == null ? null : source(faces.block(Face.UP), faces.side(Face.UP));
        return src == null ? super.getParticleIcon(data) : src.sprite();
    }

    /** The quad with its texture coordinates moved from its own sprite onto {@code to} (block vertex format: uv at 4, 5 of 8). */
    private static BakedQuad remap(BakedQuad q, TextureAtlasSprite to) {
        TextureAtlasSprite from = q.getSprite();
        int[] v = q.getVertices().clone();
        for (int i = 0; i < 4; i++) {
            float u = Float.intBitsToFloat(v[i * 8 + 4]), w = Float.intBitsToFloat(v[i * 8 + 5]);
            float ru = (u - from.getU0()) / (from.getU1() - from.getU0()), rv = (w - from.getV0()) / (from.getV1() - from.getV0());
            v[i * 8 + 4] = Float.floatToRawIntBits(to.getU0() + ru * (to.getU1() - to.getU0()));
            v[i * 8 + 5] = Float.floatToRawIntBits(to.getV0() + rv * (to.getV1() - to.getV0()));
        }
        return new BakedQuad(v, -1, q.getDirection(), to, q.isShade());
    }
}

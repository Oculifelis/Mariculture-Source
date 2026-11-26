package mariculture.platform.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.quad.MutableQuad;

import mariculture.domain.content.Face;
import mariculture.domain.factory.CustomBlocks;
import mariculture.domain.factory.CustomFaces;
import mariculture.platform.factory.CustomBlockEntity;

/**
 * Draws a sawmill block (legacy RenderCustom*): its shape's model with every face swapped for the face of the block
 * it was cut from, and see-through faces left out. Each quad keeps its place and takes the source face's texture and
 * layer.
 */
public final class CustomBlockModel extends DelegateBlockStateModel {
    /** The source quad for (block, side), cleared on every model reload. */
    private static final Map<String, BakedQuad> SOURCES = new ConcurrentHashMap<>();

    private CustomBlockModel(BlockStateModel delegate) {
        super(delegate);
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((ModelEvent.ModifyBakingResult e) -> {
            SOURCES.clear();
            e.getBakingResult().blockStateModels().replaceAll((state, model) -> isCustom(state) ? new CustomBlockModel(model) : model);
        });
    }

    private static boolean isCustom(BlockState state) {
        return CustomBlocks.isCustom(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        CustomFaces faces = level.getModelData(pos).get(CustomBlockEntity.FACES);
        if (faces == null) {
            delegate.collectParts(level, pos, state, random, parts);
            return;
        }
        List<BlockStateModelPart> base = new ArrayList<>();
        delegate.collectParts(level, pos, state, random, base);
        for (BlockStateModelPart p : base) parts.add(new Part(p, faces));
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return level.getModelData(pos).get(CustomBlockEntity.FACES) == null ? delegate.materialFlags(level, pos, state)
                : BakedQuad.FLAG_TRANSLUCENT | BakedQuad.FLAG_ANIMATED;
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        CustomFaces faces = level.getModelData(pos).get(CustomBlockEntity.FACES);
        BakedQuad top = faces == null ? null : source(faces.block(Face.UP), faces.side(Face.UP));
        if (top == null) return delegate.particleMaterial(level, pos, state);
        Block b = CustomBlockEntity.faceBlock(faces.block(Face.UP));
        return b == null ? delegate.particleMaterial(level, pos, state)
                : Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(b.defaultBlockState());
    }

    /** The quad a block shows on one side: its model's quad facing that way, culled or not. */
    static BakedQuad source(String block, Face side) {
        Block b = CustomBlockEntity.faceBlock(block);
        if (b == null) return null;
        return SOURCES.computeIfAbsent(block + "|" + side, k -> {
            Direction d = Direction.from3DDataValue(side.ordinal());
            BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(b.defaultBlockState());
            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(RandomSource.create(42L), parts);
            for (BlockStateModelPart p : parts) {
                List<BakedQuad> quads = p.getQuads(d);
                if (!quads.isEmpty()) return quads.get(0);
            }
            for (BlockStateModelPart p : parts) {
                for (BakedQuad q : p.getQuads(null)) if (q.direction() == d) return q;
            }
            for (BlockStateModelPart p : parts) {
                for (Direction any : Direction.values()) if (!p.getQuads(any).isEmpty()) return p.getQuads(any).get(0);
            }
            return null;
        });
    }

    private record Part(BlockStateModelPart base, CustomFaces faces) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            List<BakedQuad> out = new ArrayList<>();
            for (BakedQuad q : base.getQuads(direction)) {
                Face f = Face.values()[q.direction().get3DDataValue()];
                if (faces.block(f).equals(CustomFaces.AIR)) continue;
                BakedQuad src = source(faces.block(f), faces.side(f));
                if (src == null) {
                    out.add(q);
                    continue;
                }
                BakedQuad.MaterialInfo m = src.materialInfo();
                out.add(new MutableQuad().setFrom(q).setSpriteAndMoveUv(m.sprite(), m.layer(), m.itemRenderType()).setTintIndex(-1).toBakedQuad());
            }
            return out;
        }

        @Override
        @Deprecated
        public boolean useAmbientOcclusion() {
            return base.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return base.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return BakedQuad.FLAG_TRANSLUCENT | BakedQuad.FLAG_ANIMATED;
        }
    }
}

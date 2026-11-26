package mariculture.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

import mariculture.domain.content.Ids;
import mariculture.platform.entity.Rockhopper;

/** world/render/RenderRockhopper.java: the model drawn at half size, babies at a fifth. */
public class RockhopperRenderer extends MobRenderer<Rockhopper, LivingEntityRenderState, RockhopperModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Ids.MOD_ID, "textures/entity/rockhopper.png");

    public RockhopperRenderer(EntityRendererProvider.Context context) {
        super(context, new RockhopperModel(context.bakeLayer(RockhopperModel.LAYER)), 0.3f);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        float s = state.isBaby ? 0.2f : 0.5f;
        poseStack.scale(s, s, s);
    }
}

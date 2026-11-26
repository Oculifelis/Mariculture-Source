package mariculture.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import mariculture.domain.content.Ids;
import mariculture.platform.entity.SpeedBoatEntity;

/**
 * maritech/render/RenderSpeedBoat.java: flipped like any legacy model, turned by 180 + yaw, 0.1 down, then the
 * model's own 0.4 / 0.1 nudge.
 */
public class SpeedBoatRenderer extends EntityRenderer<SpeedBoatEntity, SpeedBoatRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Ids.MOD_ID, "textures/entity/speed_boat.png");
    private final SpeedBoatModel model;

    public static class State extends EntityRenderState {
        public float yRot, motor;
    }

    public SpeedBoatRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new SpeedBoatModel(context.bakeLayer(SpeedBoatModel.LAYER));
        shadowRadius = 0.5f;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpeedBoatEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        state.motor = entity.motorPos;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0, SpeedBoatEntity.RENDER_LIFT, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-(180 + state.yRot)));
        poseStack.scale(-1, -1, 1);
        poseStack.translate(0.4f, 0.1f, 0);
        collector.submitModel(model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}

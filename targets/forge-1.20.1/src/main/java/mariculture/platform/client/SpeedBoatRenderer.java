package mariculture.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import mariculture.domain.content.Ids;
import mariculture.platform.entity.SpeedBoatEntity;

/**
 * maritech/render/RenderSpeedBoat.java: flipped like any legacy model, turned by 180 + yaw, 0.1 down, then the
 * model's own 0.4 / 0.1 nudge.
 */
public class SpeedBoatRenderer extends EntityRenderer<SpeedBoatEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Ids.MOD_ID, "textures/entity/speed_boat.png");
    private final SpeedBoatModel model;

    public SpeedBoatRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new SpeedBoatModel(context.bakeLayer(SpeedBoatModel.LAYER));
        shadowRadius = 0.5f;
    }

    @Override
    public ResourceLocation getTextureLocation(SpeedBoatEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(SpeedBoatEntity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        poseStack.translate(0, SpeedBoatEntity.RENDER_LIFT, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-(180 + yaw)));
        poseStack.scale(-1, -1, 1);
        poseStack.translate(0.4f, 0.1f, 0);
        model.setupAnim(entity, 0, 0, entity.tickCount + partialTicks, 0, 0);
        model.renderToBuffer(poseStack, buffers.getBuffer(model.renderType(TEXTURE)), light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffers, light);
    }
}

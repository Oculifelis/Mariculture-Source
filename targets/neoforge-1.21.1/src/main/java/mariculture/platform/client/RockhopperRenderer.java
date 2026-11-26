package mariculture.platform.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import mariculture.domain.content.Ids;
import mariculture.platform.entity.Rockhopper;

/** world/render/RenderRockhopper.java: the model drawn at half size, babies at a fifth. */
public class RockhopperRenderer extends MobRenderer<Rockhopper, RockhopperModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Ids.MOD_ID, "textures/entity/rockhopper.png");

    public RockhopperRenderer(EntityRendererProvider.Context context) {
        super(context, new RockhopperModel(context.bakeLayer(RockhopperModel.LAYER)), 0.3f);
    }

    @Override
    public ResourceLocation getTextureLocation(Rockhopper entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(Rockhopper entity, PoseStack poseStack, float partialTick) {
        float s = entity.isBaby() ? 0.2f : 0.5f;
        poseStack.scale(s, s, s);
    }
}

package mariculture.platform.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import mariculture.domain.content.Ids;

/** world/render/ModelRockhopper.java: the same boxes, pivots and texture offsets (64x64). */
public class RockhopperModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Ids.MOD_ID, "rockhopper"), "main");
    private static final float TILT = 0.03490658503988659f, FLIPPER_X = -0.10471975511965977f;
    private final ModelPart head, leftFlipper, rightFlipper, leftAnkle, leftFoot, rightAnkle, rightFoot;

    public RockhopperModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        leftFlipper = root.getChild("left_flipper");
        rightFlipper = root.getChild("right_flipper");
        leftAnkle = root.getChild("left_ankle");
        leftFoot = root.getChild("left_foot");
        rightAnkle = root.getChild("right_ankle");
        rightFoot = root.getChild("right_foot");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Hair and beak shared the head's pivot and turn, so they hang off it.
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(30, 0).addBox(-3, -6, -4.5f, 6, 5, 7),
                PartPose.offsetAndRotation(0, 8, 0, TILT, 0, 0));
        head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(20, 15).addBox(-3, -7, -3.5f, 6, 1, 5), PartPose.ZERO);
        head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(15, 25).addBox(-2, -4, -7.5f, 4, 3, 3), PartPose.ZERO);
        root.addOrReplaceChild("body_upper", CubeListBuilder.create().texOffs(34, 24).addBox(-4, -5, -4, 8, 7, 7), PartPose.offset(0, 12, 0));
        root.addOrReplaceChild("body_lower", CubeListBuilder.create().texOffs(28, 46).addBox(-5, 0, -4, 10, 10, 8), PartPose.offset(0, 12, 0));
        root.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -5, 8, 12, 1), PartPose.offset(0, 12, 0));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 20).addBox(-3, 8.5f, 4, 6, 2, 1), PartPose.offset(0, 12, 0));
        root.addOrReplaceChild("left_flipper", CubeListBuilder.create().texOffs(0, 46).mirror().addBox(0, 0, 0, 12, 1, 5),
                PartPose.offsetAndRotation(4, 8, -3, FLIPPER_X, 0, 0.9599310885968813f));
        root.addOrReplaceChild("right_flipper", CubeListBuilder.create().texOffs(0, 46).addBox(-11, 0, 0, 11, 1, 5),
                PartPose.offsetAndRotation(-4, 8, -3, FLIPPER_X, 0, -0.9599310885968813f));
        CubeListBuilder ankle = CubeListBuilder.create().texOffs(0, 30).addBox(-1, 0, -2, 3, 1, 2);
        CubeListBuilder foot = CubeListBuilder.create().texOffs(0, 40).addBox(-1, 1, -4, 3, 1, 4);
        root.addOrReplaceChild("left_ankle", ankle, PartPose.offset(2, 22, 1));
        root.addOrReplaceChild("left_foot", foot, PartPose.offset(2, 22, 1));
        root.addOrReplaceChild("right_ankle", ankle, PartPose.offset(-3, 22, 1));
        root.addOrReplaceChild("right_foot", foot, PartPose.offset(-3, 22, 1));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        float pos = state.walkAnimationPos, speed = state.walkAnimationSpeed;
        rightAnkle.xRot = Mth.cos(pos * 0.6662f) * 1.4f * speed;
        rightFoot.xRot = rightAnkle.xRot * 2;
        leftAnkle.xRot = Mth.cos(pos * 0.6662f + Mth.PI) * 1.4f * speed;
        leftFoot.xRot = leftAnkle.xRot * 2;
        // EntityRockhopper.onLivingUpdate: the flippers (rotationWing / 10 radians) flap between 0.45 and 0.95 while
        // walking and rest at 1.25 standing; the body rocks up to 6 degrees.
        float walking = Math.min(1, speed * 2);
        float flap = 0.7f + 0.25f * Mth.sin(state.ageInTicks * 0.6f);
        float wing = Mth.lerp(walking, 1.25f, flap);
        rightFlipper.zRot = -wing;
        leftFlipper.zRot = wing;
        root().zRot = Mth.sin(state.ageInTicks * 0.5f) * 6 * Mth.DEG_TO_RAD * walking;
    }
}

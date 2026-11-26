package mariculture.platform.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

import mariculture.domain.content.Ids;
import mariculture.platform.entity.SpeedBoatEntity;

/** maritech/model/ModelSpeedBoat.java: the same boxes, rotations and texture offsets (128x64); the motor swings. */
public class SpeedBoatModel extends HierarchicalModel<SpeedBoatEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Ids.MOD_ID, "speed_boat"), "main");
    private final ModelPart root, motor;

    public SpeedBoatModel(ModelPart root) {
        this.root = root;
        motor = root.getChild("motor_end");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 45).mirror().addBox(-17, 0, -8, 32, 3, 16),
                PartPose.rotation(-0.0174533f, 0, 0f));
        root.addOrReplaceChild("window", CubeListBuilder.create().texOffs(33, 15).mirror().addBox(-17, -11, -8, 1, 5, 16),
                PartPose.rotation(-0.0174533f, 0, 0f));
        root.addOrReplaceChild("side_large_1", CubeListBuilder.create().texOffs(0, 27).mirror().addBox(-17, -7, 8, 9, 9, 2),
                PartPose.rotation(-0.0174533f, 0, 0f));
        root.addOrReplaceChild("motor_end", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(17, 10, -1, 3, 10, 2),
                PartPose.rotation(0f, 0, -0.6320364f));
        root.addOrReplaceChild("side_long_1", CubeListBuilder.create().texOffs(0, 38).mirror().addBox(-8, -4, 8, 24, 5, 2),
                PartPose.rotation(-0.0174533f, 0, 0f));
        root.addOrReplaceChild("side_large_2", CubeListBuilder.create().texOffs(0, 27).mirror().addBox(-17, -7, -10, 9, 9, 2),
                PartPose.rotation(-0.0174533f, 0, 0f));
        root.addOrReplaceChild("side_long_2", CubeListBuilder.create().texOffs(0, 38).mirror().addBox(-8, -4, -10, 24, 5, 2),
                PartPose.rotation(-0.0174533f, 0, 0f));
        root.addOrReplaceChild("front", CubeListBuilder.create().texOffs(52, 20).mirror().addBox(-20, -7, -8, 3, 9, 16),
                PartPose.rotation(-0.0174533f, 0, 0f));
        root.addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 5).mirror().addBox(15, -4, -8, 3, 6, 16),
                PartPose.rotation(-0.0174533f, 0, 0f));
        root.addOrReplaceChild("motor_head", CubeListBuilder.create().texOffs(35, 0).mirror().addBox(16, 4, -3, 5, 6, 6),
                PartPose.rotation(-0.0174533f, 0, -0.6320364f));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(SpeedBoatEntity entity, float pos, float speed, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        motor.xRot = entity.motorPos;
    }
}

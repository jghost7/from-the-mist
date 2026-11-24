package fromthemist.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/** Fat body, four ragged wings, a drooping tail with a sting. Texture layout: tools/make_textures.py. */
public class StingerModel extends EntityModel<LivingEntityRenderState> {
	private final ModelPart head;
	private final ModelPart tail;
	private final ModelPart sting;
	private final ModelPart leftFrontWing;
	private final ModelPart rightFrontWing;
	private final ModelPart leftBackWing;
	private final ModelPart rightBackWing;

	public StingerModel(final ModelPart root) {
		super(root, RenderTypes::entityTranslucent); // the wing membrane is see-through
		this.head = root.getChild("head");
		this.tail = root.getChild("tail");
		this.sting = this.tail.getChild("sting");
		this.leftFrontWing = root.getChild("left_front_wing");
		this.rightFrontWing = root.getChild("right_front_wing");
		this.leftBackWing = root.getChild("left_back_wing");
		this.rightBackWing = root.getChild("right_back_wing");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -5.0F, 6.0F, 6.0F, 10.0F), PartPose.offset(0.0F, 14.0F, 0.0F));
		root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, -2.5F, -5.0F, 6.0F, 5.0F, 5.0F), PartPose.offset(0.0F, 14.0F, -5.0F));
		PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 26).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 8.0F), PartPose.offsetAndRotation(0.0F, 14.0F, 5.0F, -0.5F, 0.0F, 0.0F));
		tail.addOrReplaceChild("sting", CubeListBuilder.create().texOffs(24, 26).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 8.0F, -0.8F, 0.0F, 0.0F));
		// Flat, long and narrow, like a dragonfly: a big front pair and a smaller pair swept back behind it.
		CubeListBuilder leftFront = CubeListBuilder.create().texOffs(0, 40).addBox(0.0F, 0.0F, -3.5F, 20.0F, 0.0F, 7.0F);
		CubeListBuilder rightFront = CubeListBuilder.create().texOffs(0, 40).mirror().addBox(-20.0F, 0.0F, -3.5F, 20.0F, 0.0F, 7.0F);
		CubeListBuilder leftBack = CubeListBuilder.create().texOffs(0, 48).addBox(0.0F, 0.0F, -2.5F, 15.0F, 0.0F, 5.0F);
		CubeListBuilder rightBack = CubeListBuilder.create().texOffs(0, 48).mirror().addBox(-15.0F, 0.0F, -2.5F, 15.0F, 0.0F, 5.0F);
		root.addOrReplaceChild("left_front_wing", leftFront, PartPose.offsetAndRotation(2.5F, 10.9F, -2.5F, 0.0F, 0.18F, 0.0F));
		root.addOrReplaceChild("right_front_wing", rightFront, PartPose.offsetAndRotation(-2.5F, 10.9F, -2.5F, 0.0F, -0.18F, 0.0F));
		root.addOrReplaceChild("left_back_wing", leftBack, PartPose.offsetAndRotation(2.5F, 10.9F, 1.5F, 0.0F, -0.55F, 0.0F));
		root.addOrReplaceChild("right_back_wing", rightBack, PartPose.offsetAndRotation(-2.5F, 10.9F, 1.5F, 0.0F, 0.55F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(final LivingEntityRenderState state) {
		super.setupAnim(state);
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
		// A fast shallow buzz. The pairs beat a quarter cycle apart and twist as they go, held in a raised V.
		float beat = state.ageInTicks * 2.6F;
		float front = Mth.cos(beat) * 0.55F;
		float back = Mth.cos(beat + Mth.HALF_PI) * 0.5F;
		this.leftFrontWing.zRot = -0.3F + front;
		this.rightFrontWing.zRot = 0.3F - front;
		this.leftBackWing.zRot = -0.15F + back;
		this.rightBackWing.zRot = 0.15F - back;
		this.leftFrontWing.xRot = Mth.sin(beat) * 0.25F;
		this.rightFrontWing.xRot = this.leftFrontWing.xRot;
		this.leftBackWing.xRot = Mth.sin(beat + Mth.HALF_PI) * 0.25F;
		this.rightBackWing.xRot = this.leftBackWing.xRot;
		this.tail.xRot = -0.5F + Mth.sin(state.ageInTicks * 0.2F) * 0.15F;
		this.sting.xRot = -0.8F + Mth.sin(state.ageInTicks * 0.2F + 1.0F) * 0.2F;
	}
}

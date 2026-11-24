package fromthemist.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** Low six-legged body, blunt head, two mandibles. Texture layout is documented in tools/make_textures.py. */
public class SkittererModel extends EntityModel<LivingEntityRenderState> {
	private static final float SPLAY = 0.95F;
	private final ModelPart head;
	private final ModelPart[] leftLegs = new ModelPart[3];
	private final ModelPart[] rightLegs = new ModelPart[3];

	public SkittererModel(final ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		for (int i = 0; i < 3; i++) {
			this.leftLegs[i] = root.getChild("left_leg_" + i);
			this.rightLegs[i] = root.getChild("right_leg_" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -3.0F, -7.0F, 10.0F, 6.0F, 14.0F), PartPose.offset(0.0F, 17.0F, 0.0F));
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 20).addBox(-4.0F, -3.0F, -6.0F, 8.0F, 6.0F, 6.0F), PartPose.offset(0.0F, 17.0F, -7.0F));
		head.addOrReplaceChild("left_mandible", CubeListBuilder.create().texOffs(32, 20).addBox(1.0F, 1.0F, -10.0F, 2.0F, 2.0F, 4.0F), PartPose.ZERO);
		head.addOrReplaceChild("right_mandible", CubeListBuilder.create().texOffs(32, 20).mirror().addBox(-3.0F, 1.0F, -10.0F, 2.0F, 2.0F, 4.0F), PartPose.ZERO);
		CubeListBuilder leg = CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F);
		for (int i = 0; i < 3; i++) {
			float z = -5.0F + i * 5.0F;
			root.addOrReplaceChild("left_leg_" + i, leg, PartPose.offsetAndRotation(5.0F, 18.0F, z, 0.0F, 0.0F, -SPLAY));
			root.addOrReplaceChild("right_leg_" + i, leg, PartPose.offsetAndRotation(-5.0F, 18.0F, z, 0.0F, 0.0F, SPLAY));
		}

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(final LivingEntityRenderState state) {
		super.setupAnim(state);
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
		float position = state.walkAnimationPos * 1.3F;
		float speed = Math.min(1.0F, state.walkAnimationSpeed);
		for (int i = 0; i < 3; i++) {
			// Alternating tripods: left front and back move with right middle.
			float phase = i % 2 == 0 ? 0.0F : Mth.PI;
			float swing = Mth.cos(position + phase) * 0.7F * speed;
			float lift = Math.max(0.0F, Mth.sin(position + phase)) * 0.35F * speed;
			this.leftLegs[i].xRot = swing;
			this.leftLegs[i].zRot = -SPLAY - lift;
			this.rightLegs[i].xRot = -swing;
			this.rightLegs[i].zRot = SPLAY + Math.max(0.0F, Mth.sin(position + phase + Mth.PI)) * 0.35F * speed;
		}
	}
}

package fromthemist.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Built small and drawn at LeviathanEntity.SCALE. A long slab of a body carried high on six jointed legs,
 * a blunt eyed lobe at the front, feelers trailing underneath. Texture layout: tools/make_textures.py.
 */
public class LeviathanModel extends EntityModel<LeviathanRenderState> {
	private static final int TENTACLES = 8;
	/** How far the thigh leans out from vertical, and the shin back in. */
	private static final float THIGH = 0.61F;
	private static final float SHIN = 0.27F;
	private static final float[] LEG_Z = {-14.0F, 0.0F, 14.0F};
	private static final float[] LEG_YAW = {0.55F, 0.0F, -0.55F};
	private final ModelPart[] leftLegs = new ModelPart[3];
	private final ModelPart[] rightLegs = new ModelPart[3];
	private final ModelPart[] leftShins = new ModelPart[3];
	private final ModelPart[] rightShins = new ModelPart[3];
	private final ModelPart body;
	private final ModelPart[] tentacles = new ModelPart[TENTACLES];
	private final ModelPart head;

	public LeviathanModel(final ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		for (int i = 0; i < 3; i++) {
			this.leftLegs[i] = root.getChild("left_leg_" + i);
			this.rightLegs[i] = root.getChild("right_leg_" + i);
			this.leftShins[i] = this.leftLegs[i].getChild("shin");
			this.rightShins[i] = this.rightLegs[i].getChild("shin");
		}

		for (int i = 0; i < TENTACLES; i++) {
			this.tentacles[i] = root.getChild("tentacle_" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// Ground is y = 24. The body rides 40 units up.
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-12.0F, -6.0F, -20.0F, 24.0F, 12.0F, 40.0F), PartPose.offset(0.0F, -16.0F, 0.0F));
		root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(40, 52).addBox(-8.0F, -5.0F, -10.0F, 16.0F, 10.0F, 10.0F), PartPose.offset(0.0F, -13.0F, -20.0F));
		CubeListBuilder thigh = CubeListBuilder.create().texOffs(0, 52).addBox(-2.0F, -24.0F, -2.0F, 4.0F, 24.0F, 4.0F);
		CubeListBuilder shin = CubeListBuilder.create().texOffs(16, 52).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 62.0F, 4.0F);
		for (int i = 0; i < 3; i++) {
			PartDefinition left = root.addOrReplaceChild("left_leg_" + i, thigh, PartPose.offsetAndRotation(11.0F, -16.0F, LEG_Z[i], 0.0F, LEG_YAW[i], THIGH));
			left.addOrReplaceChild("shin", shin, PartPose.offsetAndRotation(0.0F, -24.0F, 0.0F, 0.0F, 0.0F, -THIGH - SHIN));
			PartDefinition right = root.addOrReplaceChild("right_leg_" + i, thigh, PartPose.offsetAndRotation(-11.0F, -16.0F, LEG_Z[i], 0.0F, -LEG_YAW[i], -THIGH));
			right.addOrReplaceChild("shin", shin, PartPose.offsetAndRotation(0.0F, -24.0F, 0.0F, 0.0F, 0.0F, THIGH + SHIN));
		}

		CubeListBuilder tentacle = CubeListBuilder.create().texOffs(32, 52).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 20.0F, 2.0F);
		for (int i = 0; i < TENTACLES; i++) {
			float x = i % 2 == 0 ? -6.0F : 6.0F;
			float z = -15.0F + (i / 2) * 10.0F;
			root.addOrReplaceChild("tentacle_" + i, tentacle, PartPose.offset(x, -10.0F, z));
		}

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(final LeviathanRenderState state) {
		super.setupAnim(state);
		float stride = state.gait * Mth.TWO_PI;
		for (int i = 0; i < 3; i++) {
			// Tripod A (left front, left back, right middle) lifts through the first half of the stride and
			// lands at the half-way point; tripod B is half a stride behind. The server stomps on the same clock.
			float leftPhase = i == 1 ? Mth.PI : 0.0F;
			this.pose(this.leftLegs[i], stride + leftPhase, 1.0F, LEG_YAW[i]);
			this.pose(this.rightLegs[i], stride + leftPhase + Mth.PI, -1.0F, -LEG_YAW[i]);
		}

		for (int i = 0; i < TENTACLES; i++) {
			this.tentacles[i].xRot = Mth.sin(state.ageInTicks * 0.04F + i * 1.7F) * 0.3F + 0.15F;
			this.tentacles[i].zRot = Mth.cos(state.ageInTicks * 0.031F + i * 2.3F) * 0.25F;
		}

		this.head.xRot = 0.12F + Mth.sin(state.ageInTicks * 0.02F) * 0.05F;
		if (state.collapse > 0.0F) {
			this.collapse(state.collapse);
		}
	}

	/**
	 * Dying. The legs let go one pair after another and splay flat like a dead spider's while the body
	 * drops 34 units to lie on its belly.
	 */
	private void collapse(final float amount) {
		float fall = amount * amount;
		float drop = fall * 34.0F;
		this.body.y += drop;
		this.body.zRot = fall * 0.12F;
		this.head.y += drop;
		this.head.xRot = 0.12F + fall * 0.5F;
		for (int i = 0; i < 3; i++) {
			// Front pair first, back pair last.
			float give = Mth.clamp(amount * 1.5F - i * 0.2F, 0.0F, 1.0F);
			give *= give;
			this.leftLegs[i].y += drop;
			this.rightLegs[i].y += drop;
			this.leftLegs[i].zRot = THIGH + give * 0.75F;
			this.rightLegs[i].zRot = -(THIGH + give * 0.75F);
			this.leftLegs[i].yRot = LEG_YAW[i];
			this.rightLegs[i].yRot = -LEG_YAW[i];
			this.leftShins[i].zRot = -(THIGH + give * 0.75F) - SHIN - give * 1.12F;
			this.rightShins[i].zRot = THIGH + give * 0.75F + SHIN + give * 1.12F;
		}

		for (int i = 0; i < TENTACLES; i++) {
			this.tentacles[i].y += drop;
			this.tentacles[i].xRot = Mth.lerp(fall, this.tentacles[i].xRot, 1.5F);
		}
	}

	private void pose(final ModelPart leg, final float phase, final float side, final float yaw) {
		float lift = Math.max(0.0F, Mth.sin(phase));
		leg.zRot = side * (THIGH + lift * 0.22F);
		leg.yRot = yaw + side * Mth.cos(phase) * 0.16F;
	}
}

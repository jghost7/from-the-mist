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
 * Built small and drawn at ColossusEntity.SCALE. A deep hunched body with a ridge of spines, six pillar legs
 * that hang straight down under it, and a curtain of long feelers from a blunt lobe at the front.
 * Texture layout (256x128): tools/make_textures.py.
 */
public class ColossusModel extends EntityModel<LeviathanRenderState> {
	private static final int FEELERS = 12;
	private static final int SPINES = 6;
	/** How far the thigh leans out from straight down. */
	private static final float LEAN = 0.2F;
	private static final float[] LEG_Z = {-16.0F, 0.0F, 16.0F};
	private final ModelPart[] leftLegs = new ModelPart[3];
	private final ModelPart[] rightLegs = new ModelPart[3];
	private final ModelPart[] leftShins = new ModelPart[3];
	private final ModelPart[] rightShins = new ModelPart[3];
	private final ModelPart[] feelers = new ModelPart[FEELERS];
	private final ModelPart body;
	private final ModelPart lobe;

	public ColossusModel(final ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.lobe = this.body.getChild("lobe");
		for (int i = 0; i < 3; i++) {
			this.leftLegs[i] = root.getChild("left_leg_" + i);
			this.rightLegs[i] = root.getChild("right_leg_" + i);
			this.leftShins[i] = this.leftLegs[i].getChild("shin");
			this.rightShins[i] = this.rightLegs[i].getChild("shin");
		}

		for (int i = 0; i < FEELERS; i++) {
			this.feelers[i] = this.lobe.getChild("feeler_" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// Ground is y = 24. The legs are 62 units long, so the belly rides at y = -38.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -10.0F, -22.0F, 28.0F, 20.0F, 44.0F), PartPose.offset(0.0F, -48.0F, 0.0F));
		body.addOrReplaceChild("hump", CubeListBuilder.create().texOffs(144, 0).addBox(-10.0F, -10.0F, -12.0F, 20.0F, 10.0F, 24.0F), PartPose.offsetAndRotation(0.0F, -9.0F, 4.0F, -0.12F, 0.0F, 0.0F));
		CubeListBuilder spine = CubeListBuilder.create().texOffs(108, 64).addBox(-1.0F, -10.0F, -1.0F, 2.0F, 10.0F, 2.0F);
		for (int i = 0; i < SPINES; i++) {
			body.addOrReplaceChild("spine_" + i, spine, PartPose.offsetAndRotation(0.0F, -17.0F, -8.0F + i * 5.0F, -0.5F - i * 0.08F, 0.0F, 0.0F));
		}

		PartDefinition lobe = body.addOrReplaceChild("lobe", CubeListBuilder.create().texOffs(144, 34).addBox(-9.0F, -7.0F, -12.0F, 18.0F, 14.0F, 12.0F), PartPose.offsetAndRotation(0.0F, 4.0F, -22.0F, 0.25F, 0.0F, 0.0F));
		CubeListBuilder feeler = CubeListBuilder.create().texOffs(100, 64).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 34.0F, 2.0F);
		for (int i = 0; i < FEELERS; i++) {
			float x = -7.5F + (i % 6) * 3.0F;
			float z = i < 6 ? -9.0F : -4.0F;
			lobe.addOrReplaceChild("feeler_" + i, feeler, PartPose.offset(x, 7.0F, z));
		}

		CubeListBuilder thigh = CubeListBuilder.create().texOffs(0, 64).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 30.0F, 8.0F);
		CubeListBuilder shin = CubeListBuilder.create()
			.texOffs(32, 64).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 32.0F, 7.0F)
			.texOffs(60, 64).addBox(-5.0F, 30.0F, -5.0F, 10.0F, 3.0F, 10.0F);
		for (int i = 0; i < 3; i++) {
			PartDefinition left = root.addOrReplaceChild("left_leg_" + i, thigh, PartPose.offsetAndRotation(11.0F, -39.0F, LEG_Z[i], 0.0F, 0.0F, -LEAN));
			left.addOrReplaceChild("shin", shin, PartPose.offsetAndRotation(0.0F, 30.0F, 0.0F, 0.0F, 0.0F, LEAN));
			PartDefinition right = root.addOrReplaceChild("right_leg_" + i, thigh, PartPose.offsetAndRotation(-11.0F, -39.0F, LEG_Z[i], 0.0F, 0.0F, LEAN));
			right.addOrReplaceChild("shin", shin, PartPose.offsetAndRotation(0.0F, 30.0F, 0.0F, 0.0F, 0.0F, -LEAN));
		}

		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(final LeviathanRenderState state) {
		super.setupAnim(state);
		float stride = state.gait * Mth.TWO_PI;
		for (int i = 0; i < 3; i++) {
			// Tripod A (left front, left back, right middle) is in the air for the first half of the stride and
			// lands at the half-way point; tripod B is half a stride behind. The server stomps on the same clock.
			float phase = stride + (i == 1 ? Mth.PI : 0.0F);
			pose(this.leftLegs[i], this.leftShins[i], phase);
			pose(this.rightLegs[i], this.rightShins[i], phase + Mth.PI);
		}

		// The whole body settles a little each time a tripod lands.
		this.body.y = -48.0F + Math.abs(Mth.sin(stride)) * 1.2F;
		this.body.zRot = Mth.sin(stride) * 0.025F;
		this.lobe.xRot = 0.25F + Mth.sin(state.ageInTicks * 0.015F) * 0.06F;
		for (int i = 0; i < FEELERS; i++) {
			this.feelers[i].xRot = Mth.sin(state.ageInTicks * 0.035F + i * 1.3F) * 0.22F - 0.1F;
			this.feelers[i].zRot = Mth.cos(state.ageInTicks * 0.027F + i * 2.1F) * 0.2F;
		}

		if (state.collapse > 0.0F) {
			this.collapse(state.collapse);
		}
	}

	/**
	 * Dying. The pillar legs buckle outward one pair after another and the body comes straight down 62 units
	 * onto its belly, legs flat on the ground either side, feelers spilled out in front.
	 */
	private void collapse(final float amount) {
		float fall = amount * amount;
		float drop = fall * 62.0F;
		this.body.y = -48.0F + drop;
		this.body.zRot = fall * 0.06F;
		this.body.xRot = fall * 0.05F;
		this.lobe.xRot = 0.25F + fall * 0.3F;
		for (int i = 0; i < 3; i++) {
			float give = Mth.clamp(amount * 1.5F - i * 0.2F, 0.0F, 1.0F);
			give *= give;
			float lean = LEAN + give * 1.3F;          // thigh goes from hanging down to lying out sideways
			float spread = give * (i - 1) * 0.55F;    // front pair forward, back pair backward
			this.leftLegs[i].y += drop;
			this.rightLegs[i].y += drop;
			this.leftLegs[i].zRot = -lean;
			this.rightLegs[i].zRot = lean;
			this.leftLegs[i].xRot = 0.0F;
			this.rightLegs[i].xRot = 0.0F;
			this.leftLegs[i].yRot = spread;
			this.rightLegs[i].yRot = -spread;
			this.leftShins[i].xRot = 0.0F;
			this.rightShins[i].xRot = 0.0F;
			this.leftShins[i].zRot = LEAN * (1.0F - give) - give * 0.2F;
			this.rightShins[i].zRot = -LEAN * (1.0F - give) + give * 0.2F;
		}

		for (int i = 0; i < FEELERS; i++) {
			this.feelers[i].xRot = Mth.lerp(fall, this.feelers[i].xRot, -1.2F);
			this.feelers[i].zRot *= 1.0F - fall;
		}
	}

	/** A slow piston step: the thigh swings forward as it lifts and the knee folds, then the leg straightens and plants. */
	private static void pose(final ModelPart thigh, final ModelPart shin, final float phase) {
		float lift = Math.max(0.0F, Mth.sin(phase));
		thigh.xRot = -Mth.cos(phase) * 0.14F - lift * 0.16F;
		shin.xRot = lift * 0.34F;
	}
}

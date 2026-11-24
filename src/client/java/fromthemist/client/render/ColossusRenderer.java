package fromthemist.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import fromthemist.FromTheMist;
import fromthemist.client.FromTheMistClient;
import fromthemist.entity.ColossusEntity;
import fromthemist.entity.LeviathanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

public class ColossusRenderer extends MobRenderer<ColossusEntity, LeviathanRenderState, ColossusModel> {
	private static final Identifier TEXTURE = FromTheMist.id("textures/entity/colossus.png");

	public ColossusRenderer(final EntityRendererProvider.Context context) {
		super(context, new ColossusModel(context.bakeLayer(FromTheMistClient.COLOSSUS_LAYER)), 0.0F);
	}

	@Override
	public LeviathanRenderState createRenderState() {
		return new LeviathanRenderState();
	}

	@Override
	public void extractRenderState(final ColossusEntity entity, final LeviathanRenderState state, final float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.gait = (entity.level().getGameTime() % ColossusEntity.GAIT_TICKS + partialTicks) / ColossusEntity.GAIT_TICKS;
		state.collapse = Math.min(1.0F, state.deathTime / LeviathanEntity.COLLAPSE_TICKS);
		state.hasRedOverlay = entity.hurtTime > 0;
	}

	@Override
	protected float getFlipDegrees() {
		return 0.0F; // it comes straight down (see the model), it does not tip over
	}

	@Override
	protected void scale(final LeviathanRenderState state, final PoseStack poseStack) {
		poseStack.scale(ColossusEntity.SCALE, ColossusEntity.SCALE, ColossusEntity.SCALE);
	}

	@Override
	protected AABB getBoundingBoxForCulling(final ColossusEntity entity, final float partialTicks) {
		return entity.getBoundingBox().inflate(14.0, 8.0, 14.0);
	}

	@Override
	public Identifier getTextureLocation(final LeviathanRenderState state) {
		return TEXTURE;
	}
}

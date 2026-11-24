package fromthemist.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import fromthemist.FromTheMist;
import fromthemist.client.FromTheMistClient;
import fromthemist.entity.LeviathanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

public class LeviathanRenderer extends MobRenderer<LeviathanEntity, LeviathanRenderState, LeviathanModel> {
	private static final Identifier TEXTURE = FromTheMist.id("textures/entity/leviathan.png");

	public LeviathanRenderer(final EntityRendererProvider.Context context) {
		super(context, new LeviathanModel(context.bakeLayer(FromTheMistClient.LEVIATHAN_LAYER)), 0.0F);
	}

	@Override
	public LeviathanRenderState createRenderState() {
		return new LeviathanRenderState();
	}

	@Override
	public void extractRenderState(final LeviathanEntity entity, final LeviathanRenderState state, final float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.gait = (entity.level().getGameTime() % LeviathanEntity.GAIT_TICKS + partialTicks) / LeviathanEntity.GAIT_TICKS;
		state.collapse = Math.min(1.0F, state.deathTime / LeviathanEntity.COLLAPSE_TICKS);
		state.hasRedOverlay = entity.hurtTime > 0;
	}

	@Override
	protected float getFlipDegrees() {
		return 0.0F; // it comes straight down (see the model), it does not tip over
	}

	@Override
	protected void scale(final LeviathanRenderState state, final PoseStack poseStack) {
		poseStack.scale(LeviathanEntity.SCALE, LeviathanEntity.SCALE, LeviathanEntity.SCALE);
	}

	@Override
	protected AABB getBoundingBoxForCulling(final LeviathanEntity entity, final float partialTicks) {
		// The legs stand far outside the body's own box.
		return entity.getBoundingBox().inflate(20.0, 6.0, 20.0);
	}

	@Override
	public Identifier getTextureLocation(final LeviathanRenderState state) {
		return TEXTURE;
	}
}

package fromthemist.client.render;

import fromthemist.FromTheMist;
import fromthemist.entity.HollowEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** The Hollow use the ordinary player shape on purpose: from a distance they are just people. */
public class HollowRenderer extends HumanoidMobRenderer<HollowEntity, HollowRenderer.State, HumanoidModel<HollowRenderer.State>> {
	private static final Identifier[] TEXTURES = {
		FromTheMist.id("textures/entity/hollow_1.png"),
		FromTheMist.id("textures/entity/hollow_2.png"),
		FromTheMist.id("textures/entity/hollow_3.png"),
		FromTheMist.id("textures/entity/hollow_4.png")
	};

	public HollowRenderer(final EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(final HollowEntity entity, final State state, final float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.variant = Math.floorMod(entity.getUUID().hashCode(), TEXTURES.length);
	}

	@Override
	public Identifier getTextureLocation(final State state) {
		return TEXTURES[state.variant];
	}

	public static class State extends HumanoidRenderState {
		public int variant;
	}
}

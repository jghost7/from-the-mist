package fromthemist.client.render;

import fromthemist.FromTheMist;
import fromthemist.client.FromTheMistClient;
import fromthemist.entity.SkittererEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class SkittererRenderer extends MobRenderer<SkittererEntity, LivingEntityRenderState, SkittererModel> {
	private static final Identifier TEXTURE = FromTheMist.id("textures/entity/skitterer.png");

	public SkittererRenderer(final EntityRendererProvider.Context context) {
		super(context, new SkittererModel(context.bakeLayer(FromTheMistClient.SKITTERER_LAYER)), 0.6F);
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	public Identifier getTextureLocation(final LivingEntityRenderState state) {
		return TEXTURE;
	}
}

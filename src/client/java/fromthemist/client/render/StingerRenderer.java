package fromthemist.client.render;

import fromthemist.FromTheMist;
import fromthemist.client.FromTheMistClient;
import fromthemist.entity.StingerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class StingerRenderer extends MobRenderer<StingerEntity, LivingEntityRenderState, StingerModel> {
	private static final Identifier TEXTURE = FromTheMist.id("textures/entity/stinger.png");

	public StingerRenderer(final EntityRendererProvider.Context context) {
		super(context, new StingerModel(context.bakeLayer(FromTheMistClient.STINGER_LAYER)), 0.4F);
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

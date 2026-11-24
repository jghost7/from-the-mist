package fromthemist.client;

import fromthemist.FromTheMist;
import fromthemist.MistPayload;
import fromthemist.ModContent;
import fromthemist.ShakePayload;
import fromthemist.client.render.ColossusModel;
import fromthemist.client.render.ColossusRenderer;
import fromthemist.client.render.HollowRenderer;
import fromthemist.client.render.LeviathanModel;
import fromthemist.client.render.LeviathanRenderer;
import fromthemist.client.render.SkittererModel;
import fromthemist.client.render.SkittererRenderer;
import fromthemist.client.render.StingerModel;
import fromthemist.client.render.StingerRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;

public class FromTheMistClient implements ClientModInitializer {
	public static final ModelLayerLocation SKITTERER_LAYER = new ModelLayerLocation(FromTheMist.id("skitterer"), "main");
	public static final ModelLayerLocation STINGER_LAYER = new ModelLayerLocation(FromTheMist.id("stinger"), "main");
	public static final ModelLayerLocation LEVIATHAN_LAYER = new ModelLayerLocation(FromTheMist.id("leviathan"), "main");

	public static final ModelLayerLocation COLOSSUS_LAYER = new ModelLayerLocation(FromTheMist.id("colossus"), "main");

	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(COLOSSUS_LAYER, ColossusModel::createLayer);
		EntityRendererRegistry.register(ModContent.COLOSSUS, ColossusRenderer::new);
		ClientPlayNetworking.registerGlobalReceiver(ShakePayload.TYPE, (payload, context) -> MistFog.shake = Math.max(MistFog.shake, payload.strength()));
		ModelLayerRegistry.registerModelLayer(SKITTERER_LAYER, SkittererModel::createLayer);
		ModelLayerRegistry.registerModelLayer(STINGER_LAYER, StingerModel::createLayer);
		ModelLayerRegistry.registerModelLayer(LEVIATHAN_LAYER, LeviathanModel::createLayer);

		EntityRendererRegistry.register(ModContent.HOLLOW, HollowRenderer::new);
		EntityRendererRegistry.register(ModContent.SKITTERER, SkittererRenderer::new);
		EntityRendererRegistry.register(ModContent.STINGER, StingerRenderer::new);
		EntityRendererRegistry.register(ModContent.LEVIATHAN, LeviathanRenderer::new);

		ClientPlayNetworking.registerGlobalReceiver(MistPayload.TYPE, (payload, context) -> MistFog.target = payload.intensity());
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> MistFog.reset());
		ClientTickEvents.END_CLIENT_TICK.register(MistFog::tick);
		RpgClient.init();
		ClientTickEvents.END_CLIENT_TICK.register(RpgClient::tick);
	}
}

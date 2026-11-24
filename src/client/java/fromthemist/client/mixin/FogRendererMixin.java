package fromthemist.client.mixin;

import fromthemist.client.MistFog;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
	@Inject(method = "setupFog", at = @At("RETURN"))
	private void fromthemist$closeIn(
		final Camera camera,
		final int renderDistanceInChunks,
		final DeltaTracker deltaTracker,
		final float darkenWorldAmount,
		final ClientLevel level,
		final CallbackInfoReturnable<FogData> info
	) {
		MistFog.apply(info.getReturnValue(), camera, level, deltaTracker.getGameTimeDeltaPartialTick(false));
	}
}

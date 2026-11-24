package fromthemist.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;

/** Client side of the mist: eases toward the thickness the server reports and closes the fog in. */
public final class MistFog {
	/** How far you can see at full mist, in blocks. */
	private static final float MIST_END = 34.0F;
	private static final float MIST_START = 1.5F;
	/** Thickness last reported by the server. */
	public static float target;
	/** Set when a leviathan's foot lands nearby; decays over the next half second. */
	public static float shake;
	private static float previous;
	private static float current;

	private MistFog() {
	}

	public static void reset() {
		target = 0.0F;
		shake = 0.0F;
		previous = 0.0F;
		current = 0.0F;
	}

	public static void tick(final Minecraft client) {
		previous = current;
		if (client.level == null || client.player == null) {
			return;
		}

		if (shake > 0.02F && !client.isPaused()) {
			// Jolt the view back and forth; the alternating sign keeps the aim where it was.
			float jolt = (client.player.tickCount % 2 == 0 ? 1.0F : -1.0F) * shake * 9.0F;
			client.player.turn(jolt * 0.4F, jolt);
			shake *= 0.78F;
		} else {
			shake = 0.0F;
		}

		float goal = client.level.dimension() == Level.OVERWORLD ? target : 0.0F;
		current += Mth.clamp(goal - current, -0.01F, 0.01F);

		// Drifting motes so the mist has something moving in it up close.
		float strength = current * depthFade((float) client.player.getY(), client.level);
		if (strength > 0.3F && !client.isPaused()) {
			RandomSource random = client.level.getRandom();
			for (int i = 0; i < 3; i++) {
				double x = client.player.getX() + (random.nextDouble() - 0.5) * 24.0;
				double y = client.player.getY() + random.nextDouble() * 8.0 - 1.0;
				double z = client.player.getZ() + (random.nextDouble() - 0.5) * 24.0;
				client.level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0.0, 0.0, 0.0);
			}
		}
	}

	/** The mist lies on the surface: full strength near sea level and above, gone a little way underground. */
	private static float depthFade(final float y, final ClientLevel level) {
		return Mth.clamp((y - (level.getSeaLevel() - 26)) / 14.0F, 0.0F, 1.0F);
	}

	/** Called from the fog renderer after vanilla has worked out its own fog. */
	public static void apply(final FogData fog, final Camera camera, final ClientLevel level, final float partialTick) {
		if (camera.getFluidInCamera() != FogType.NONE || level.dimension() != Level.OVERWORLD) {
			return;
		}

		float mist = Mth.lerp(partialTick, previous, current) * depthFade((float) camera.position().y, level);
		if (mist <= 0.001F) {
			return;
		}

		// Geometric blend so the fog wall walks in steadily instead of snapping shut at the end.
		float end = Math.min(fog.environmentalEnd, 512.0F);
		float sight = Math.min(end, MIST_END * RpgClient.mistSight()); // Mist Eyes and Clear Air push the wall back
		fog.environmentalEnd = end * (float) Math.pow(sight / end, mist);
		fog.environmentalStart = Mth.lerp(mist, Math.min(fog.environmentalStart, fog.environmentalEnd), MIST_START);
		// The sky dome sits only a few blocks overhead, so it needs a far shorter fog to disappear when you look up.
		float sky = Math.min(fog.skyEnd, 512.0F);
		fog.skyEnd = Math.min(fog.skyEnd, sky * (float) Math.pow(1.5F / sky, mist));
		fog.cloudEnd = Math.min(fog.cloudEnd, fog.environmentalEnd * 1.5F);

		// Pale grey by day; at night it keeps just enough light to show shapes moving in it.
		float daylight = Mth.clamp(Math.max(fog.color.x, Math.max(fog.color.y, fog.color.z)), 0.0F, 1.0F);
		float brightness = 0.1F + 0.9F * daylight;
		fog.color.x = Mth.lerp(mist, fog.color.x, 0.78F * brightness);
		fog.color.y = Mth.lerp(mist, fog.color.y, 0.80F * brightness);
		fog.color.z = Mth.lerp(mist, fog.color.z, 0.82F * brightness);
	}
}

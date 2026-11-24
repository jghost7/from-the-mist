package fromthemist.entity;

import fromthemist.ModContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The heavy leviathan. Half as tall again as the Strider, on six pillar legs, with a curtain of feelers
 * hanging from its front. It sounds like a foghorn, the ground jumps when it steps, and Stingers travel with it.
 */
public class ColossusEntity extends LeviathanEntity {
	public static final int GAIT_TICKS = 120;
	public static final float SCALE = 8.0F;

	public ColossusEntity(final EntityType<? extends ColossusEntity> type, final Level level) {
		super(type, level);
	}

	@Override
	public int gaitTicks() {
		return GAIT_TICKS;
	}

	@Override
	protected double speed() {
		return 0.075;
	}

	@Override
	protected double footSide() {
		return 9.0;
	}

	@Override
	protected double footForward() {
		return 8.0;
	}

	@Override
	protected double stompRadius() {
		return 4.5;
	}

	@Override
	protected float stompDamage() {
		return 40.0F;
	}

	@Override
	protected double shakeRange() {
		return 72.0;
	}

	@Override
	protected void call(final ServerLevel level) {
		if (this.life % 420 == 60) {
			// A goat horn dropped two octaves carries a long way and sounds like a ship in fog.
			level.playSound(null, this.getX(), this.getY() + 30.0, this.getZ(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(2).value(), SoundSource.HOSTILE, 16.0F, 0.5F);
		}
	}

	@Override
	protected void serverStep(final ServerLevel level) {
		// Stingers ride its legs and peel off around it. Following one is dangerous and worth it.
		if (this.life % 160 != 80 || level.getEntitiesOfClass(StingerEntity.class, this.getBoundingBox().inflate(30.0, 10.0, 30.0)).size() >= 6) {
			return;
		}

		for (int i = 0; i < 2; i++) {
			StingerEntity stinger = ModContent.STINGER.create(level, EntitySpawnReason.EVENT);
			if (stinger == null) {
				return;
			}

			float angle = this.random.nextFloat() * Mth.TWO_PI;
			stinger.snapTo(this.getX() + Mth.cos(angle) * 9.0, this.getY() + 8.0 + this.random.nextInt(12), this.getZ() + Mth.sin(angle) * 9.0, angle * Mth.RAD_TO_DEG, 0.0F);
			level.addFreshEntity(stinger);
		}
	}
}

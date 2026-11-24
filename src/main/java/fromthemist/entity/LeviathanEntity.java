package fromthemist.entity;

import fromthemist.ModSounds;
import fromthemist.MistDirector;
import fromthemist.ShakePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Something the size of a building, crossing the world on six legs with no interest in you at all.
 * It walks a straight line through the mist, steps over forests, and kills whatever is under a foot when it lands.
 * This class is the Strider, the lighter spider-legged kind; ColossusEntity is the heavy one.
 */
public class LeviathanEntity extends Mob {
	/** Ticks for one full stride; each tripod of legs lands once per stride. Shared with the client model. */
	public static final int GAIT_TICKS = 80;
	/** Model scale. Foot positions are in blocks at this scale. */
	public static final float SCALE = 6.0F;
	/** Dying: this many ticks for the legs to give way and the body to come down, then it lies there before it goes. */
	public static final int COLLAPSE_TICKS = 60;
	public static final int LIE_TICKS = 50;

	private float heading;
	protected int life;

	public LeviathanEntity(final EntityType<? extends LeviathanEntity> type, final Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 500.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	// ---- what differs between kinds of leviathan

	public int gaitTicks() {
		return GAIT_TICKS;
	}

	protected double speed() {
		return 0.11;
	}

	/** How far out to the side, and how far fore and aft, the feet land (blocks). */
	protected double footSide() {
		return 15.0;
	}

	protected double footForward() {
		return 9.0;
	}

	protected double stompRadius() {
		return 3.0;
	}

	protected float stompDamage() {
		return 18.0F;
	}

	protected float stepPitch() {
		return 1.0F;
	}

	/** How far away a footfall can be felt, in blocks. */
	protected double shakeRange() {
		return 40.0;
	}

	protected void call(final ServerLevel level) {
		if (this.life % 360 == 120) {
			level.playSound(null, this.getX(), this.getY() + 16.0, this.getZ(), ModSounds.LEVIATHAN_CALL, SoundSource.HOSTILE, 10.0F, 1.0F);
		}
	}

	/** Anything else this kind does as it walks. */
	protected void serverStep(final ServerLevel level) {
	}

	// ---- shared behaviour

	public void setHeading(final float heading) {
		this.heading = heading;
		this.setYRot(heading);
		this.yBodyRot = heading;
		this.yHeadRot = heading;
	}

	@Override
	public void aiStep() {
		if (!(this.level() instanceof ServerLevel level)) {
			super.aiStep();
			return;
		}

		this.noPhysics = true;
		if (this.isDeadOrDying()) {
			return; // it has stopped walking; tickDeath brings it down
		}

		this.life++;
		double radians = this.heading * Mth.DEG_TO_RAD;
		double forwardX = -Math.sin(radians);
		double forwardZ = Math.cos(radians);
		double x = this.getX() + forwardX * this.speed();
		double z = this.getZ() + forwardZ * this.speed();
		BlockPos column = BlockPos.containing(x, this.getY(), z);
		if (!level.isLoaded(column)) {
			this.discard();
			return;
		}

		double ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
		this.setPos(x, Mth.lerp(0.03, this.getY(), ground), z);
		this.setHeading(this.heading);

		long time = level.getGameTime();
		int gait = this.gaitTicks();
		if (time % (gait / 2) == 0L) {
			this.stomp(level, time % gait == 0L ? 1.0 : -1.0, forwardX, forwardZ);
		}

		this.call(level);
		this.serverStep(level);
		if (this.life % 100 == 0 && (this.life > 8000 || MistDirector.intensityAt(this) < 0.1F || level.getNearestPlayer(this, 240.0) == null)) {
			this.discard();
		}
	}

	/** One tripod of feet comes down: front and back on one side, middle on the other. */
	private void stomp(final ServerLevel level, final double side, final double forwardX, final double forwardZ) {
		double rightX = -forwardZ;
		double rightZ = forwardX;
		double[][] feet = {
			{side * this.footSide(), this.footForward()},
			{side * this.footSide(), -this.footForward()},
			{-side * this.footSide(), 0.0}
		};
		double radius = this.stompRadius();
		for (double[] foot : feet) {
			double x = this.getX() + rightX * foot[0] + forwardX * foot[1];
			double z = this.getZ() + rightZ * foot[0] + forwardZ * foot[1];
			int column = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
			level.playSound(null, x, column, z, ModSounds.LEVIATHAN_STEP, SoundSource.HOSTILE, 6.0F, this.stepPitch());
			level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, column + 0.5, z, (int) (radius * 8.0), radius * 0.5, 0.4, radius * 0.5, 0.03);
			AABB area = new AABB(x - radius, column - 3.0, z - radius, x + radius, column + 5.0, z + radius);
			for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, e -> !(e instanceof LeviathanEntity || e instanceof StingerEntity || e instanceof SkittererEntity))) {
				victim.hurtServer(level, this.damageSources().mobAttack(this), this.stompDamage());
			}

			// The ground jumps under anyone near where the foot lands.
			double range = this.shakeRange();
			for (ServerPlayer player : level.players()) {
				double distance = Math.sqrt(player.distanceToSqr(x, column, z));
				if (distance < range) {
					ServerPlayNetworking.send(player, new ShakePayload((float) (1.0 - distance / range)));
				}
			}
		}
	}

	/** No tipping over: the legs buckle, the body drops where it stands, and the carcass dissolves into the mist. */
	@Override
	protected void tickDeath() {
		this.deathTime++;
		if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) {
			return;
		}

		double reach = this.footSide();
		if (this.deathTime == 1) {
			level.playSound(null, this.getX(), this.getY() + 12.0, this.getZ(), ModSounds.LEVIATHAN_CALL, SoundSource.HOSTILE, 12.0F, 0.6F); // its own call, dragged down
		}

		if (this.deathTime < COLLAPSE_TICKS && this.deathTime % 6 == 0) {
			// Joints letting go on the way down.
			level.playSound(null, this.getX(), this.getY() + 6.0, this.getZ(), ModSounds.LEVIATHAN_CRACK, SoundSource.HOSTILE, 6.0F, 0.7F + this.random.nextFloat() * 0.3F);
			level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 2.0, this.getZ(), 30, reach * 0.6, 1.5, reach * 0.6, 0.05);
		}

		if (this.deathTime == COLLAPSE_TICKS) {
			// The body hits the ground.
			level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.LEVIATHAN_CRASH, SoundSource.HOSTILE, 10.0F, this.stepPitch());
			level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 160, reach * 0.7, 1.0, reach * 0.7, 0.06);
			level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.0, this.getZ(), 12, reach * 0.5, 0.5, reach * 0.5, 0.0);
			AABB under = new AABB(this.getX() - reach * 0.6, this.getY() - 2.0, this.getZ() - reach * 0.6, this.getX() + reach * 0.6, this.getY() + 5.0, this.getZ() + reach * 0.6);
			for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, under, e -> !(e instanceof LeviathanEntity))) {
				victim.hurtServer(level, this.damageSources().mobAttack(this), this.stompDamage());
			}

			for (ServerPlayer player : level.players()) {
				if (player.distanceToSqr(this) < 96.0 * 96.0) {
					ServerPlayNetworking.send(player, new ShakePayload(1.0F));
				}
			}
		}

		if (this.deathTime > COLLAPSE_TICKS && this.deathTime % 5 == 0) {
			level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 1.5, this.getZ(), 40, reach * 0.6, 1.2, reach * 0.6, 0.02);
		}

		if (this.deathTime >= COLLAPSE_TICKS + LIE_TICKS) {
			level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 2.0, this.getZ(), 300, reach * 0.7, 2.0, reach * 0.7, 0.08);
			this.remove(Entity.RemovalReason.KILLED);
		}
	}

	@Override
	public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
		return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, damage);
	}

	@Override
	protected void addAdditionalSaveData(final ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putFloat("Heading", this.heading);
		output.putInt("Life", this.life);
	}

	@Override
	protected void readAdditionalSaveData(final ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setHeading(input.getFloatOr("Heading", 0.0F));
		this.life = input.getIntOr("Life", 0);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public void push(final Entity entity) {
	}

	@Override
	protected void doPush(final Entity entity) {
	}

	@Override
	public boolean requiresCustomPersistence() {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(final double distSqr) {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(final double distance) {
		return true;
	}
}

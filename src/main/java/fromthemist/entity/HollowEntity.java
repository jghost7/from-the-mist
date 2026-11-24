package fromthemist.entity;

import java.util.ArrayList;
import java.util.List;

import fromthemist.MistDirector;
import fromthemist.ModSounds;
import fromthemist.WardManager;
import fromthemist.rpg.Rpg;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The night folk. They look like people, they never hurry, they cannot be killed, and they are gone at dawn.
 * They know where you are through any wall, and walls do not stop them: they pull a house apart to reach you
 * unless the room is sealed behind a talisman. They get worse with every few nights survived (MistState.stage).
 */
public class HollowEntity extends Monster {
	private static final double SENSE_RANGE = 48.0;
	private Vec3 lastCheck = Vec3.ZERO;
	private @Nullable BlockPos breaking;
	private int breakProgress;
	private int knockCooldown = 60;
	private int lastAttackTick = -1000;

	public HollowEntity(final EntityType<? extends HollowEntity> type, final Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 40.0)
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.ATTACK_DAMAGE, 11.0)
			.add(Attributes.FOLLOW_RANGE, SENSE_RANGE)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.9);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
	}

	@Override
	public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
		// Nothing a player carries can kill one. Only /kill and the void get through.
		return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, damage);
	}

	@Override
	public boolean doHurtTarget(final ServerLevel level, final Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit) {
			this.lastAttackTick = this.tickCount;
		}

		return hit;
	}

	@Override
	protected void customServerAiStep(final ServerLevel level) {
		super.customServerAiStep(level);
		if (this.tickCount % 20 != 0) {
			return;
		}

		if (!MistDirector.isNight(level) || level.getNearestPlayer(this, 128.0) == null) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 24, 0.3, 0.7, 0.3, 0.01);
			this.discard();
			return;
		}

		// Every few nights survived they are quicker, stronger, and through a wall sooner.
		int stage = MistDirector.stage(level);
		this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2 + 0.025 * stage);
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(11.0 + 2.0 * stage);

		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive() || this.distanceToSqr(target) > 64.0 * 64.0 || target instanceof Player player && (player.isCreative() || player.isSpectator())) {
			// No line of sight needed: they simply know.
			target = Rpg.nearestSensed(level, this, SENSE_RANGE);
			this.setTarget(target);
		}

		if (target == null) {
			this.stopBreaking(level);
			return;
		}

		if (WardManager.isWarded(level, target.blockPosition()) && this.distanceToSqr(target) < 100.0) {
			// Kept out: stand at the wall, watch, and knock.
			this.getLookControl().setLookAt(target, 30.0F, 30.0F);
			this.knockCooldown -= 20;
			if (this.knockCooldown <= 0) {
				this.knockCooldown = 80 + this.random.nextInt(160);
				this.playSound(ModSounds.HOLLOW_KNOCK, 1.2F, 0.85F + this.random.nextFloat() * 0.2F);
			}
		}

		// Not getting anywhere and not landing blows: something is in the way, so take it apart.
		Vec3 here = this.position();
		boolean moved = here.distanceToSqr(this.lastCheck) >= 0.16;
		this.lastCheck = here;
		BlockPos obstacle = moved || this.tickCount - this.lastAttackTick < 50 || !level.getGameRules().get(GameRules.MOB_GRIEFING)
			? null
			: this.findObstacle(level, target, stage);
		if (obstacle == null) {
			this.stopBreaking(level);
			return;
		}

		if (!obstacle.equals(this.breaking)) {
			this.stopBreaking(level);
			this.breaking = obstacle;
		}

		float hardness = level.getBlockState(obstacle).getDestroySpeed(level, obstacle);
		int needed = Math.max(1, Math.round((2.0F + hardness * 2.0F) / (1.0F + 0.5F * stage))); // seconds
		this.breakProgress++;
		this.playSound(SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.9F, 0.7F);
		level.destroyBlockProgress(this.getId(), obstacle, Math.min(9, this.breakProgress * 10 / needed));
		if (this.breakProgress >= needed) {
			level.destroyBlock(obstacle, true, this, 512);
			this.playSound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.0F, 0.6F);
			this.stopBreaking(level);
		}
	}

	private void stopBreaking(final ServerLevel level) {
		if (this.breaking != null) {
			level.destroyBlockProgress(this.getId(), this.breaking, -1);
			this.breaking = null;
		}

		this.breakProgress = 0;
	}

	/** The block between this Hollow and its target that it can and may break, or null. */
	private @Nullable BlockPos findObstacle(final ServerLevel level, final LivingEntity target, final int stage) {
		BlockPos feet = this.blockPosition();
		double dx = target.getX() - this.getX();
		double dy = target.getY() - this.getY();
		double dz = target.getZ() - this.getZ();
		BlockPos ahead = feet.relative(Direction.getApproximateNearest(dx, 0.0, dz));
		List<BlockPos> candidates = new ArrayList<>();
		candidates.add(firstSolid(level, this.getEyePosition(), target.getEyePosition()));
		candidates.add(firstSolid(level, this.position().add(0.0, 0.5, 0.0), target.position().add(0.0, 0.5, 0.0)));
		if (dy > 0.5) {
			candidates.add(feet.above(2)); // head room to climb
			candidates.add(ahead.above(2));
		}

		candidates.add(ahead.above());
		candidates.add(ahead);
		if (dy < -1.0) {
			candidates.add(ahead.below());
			candidates.add(feet.below());
		}

		for (BlockPos pos : candidates) {
			if (pos == null) {
				continue;
			}

			BlockState state = level.getBlockState(pos);
			if (state.isAir() || state.getCollisionShape(level, pos).isEmpty()) {
				continue;
			}

			float hardness = state.getDestroySpeed(level, pos);
			if (hardness >= 0.0F && hardness <= 3.0F + stage && !WardManager.isProtected(level, pos)) {
				return pos;
			}
		}

		return null;
	}

	/** The first block with a collision shape within four blocks along a straight line, or null. */
	static @Nullable BlockPos firstSolid(final ServerLevel level, final Vec3 from, final Vec3 to) {
		Vec3 delta = to.subtract(from);
		double length = delta.length();
		if (length < 0.01) {
			return null;
		}

		Vec3 step = delta.scale(0.25 / length);
		Vec3 point = from;
		for (double travelled = 0.0; travelled < Math.min(length, 4.0); travelled += 0.25) {
			point = point.add(step);
			BlockPos pos = BlockPos.containing(point);
			if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
				return pos;
			}
		}

		return null;
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
	public int getAmbientSoundInterval() {
		return 260;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return ModSounds.HOLLOW_IDLE;
	}

	@Override
	protected SoundEvent getHurtSound(final DamageSource source) {
		return ModSounds.HOLLOW_IDLE; // hitting one gets you a whisper, nothing more
	}

	@Override
	public float getVoicePitch() {
		return 0.9F + this.random.nextFloat() * 0.15F;
	}
}

package fromthemist.entity;

import fromthemist.MistDirector;
import fromthemist.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Fast, low, six-legged pack hunter of the mist. Killable, and the main source of mist essence. */
public class SkittererEntity extends Monster {
	public SkittererEntity(final EntityType<? extends SkittererEntity> type, final Level level) {
		super(type, level);
		this.xpReward = 15;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 14.0)
			.add(Attributes.MOVEMENT_SPEED, 0.33)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.42F));
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	protected void customServerAiStep(final ServerLevel level) {
		super.customServerAiStep(level);
		dissolveOutsideMist(this, level);
	}

	/** Mist creatures cannot hold together in clear air, so they never follow you out of the bank. */
	static void dissolveOutsideMist(final Monster mob, final ServerLevel level) {
		if (mob.tickCount % 40 == 20 && (MistDirector.intensityAt(mob) < 0.1F || level.getNearestPlayer(mob, 128.0) == null)) {
			level.sendParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY() + 0.4, mob.getZ(), 16, 0.3, 0.3, 0.3, 0.02);
			mob.discard();
		}
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
	protected SoundEvent getAmbientSound() {
		return ModSounds.SKITTERER_IDLE;
	}

	@Override
	protected SoundEvent getHurtSound(final DamageSource source) {
		return ModSounds.SKITTERER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.SKITTERER_HURT;
	}

	@Override
	public float getVoicePitch() {
		return this.isDeadOrDying() ? 0.7F : 0.9F + this.random.nextFloat() * 0.25F;
	}
}

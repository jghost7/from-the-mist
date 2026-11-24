package fromthemist.entity;

import fromthemist.rpg.Rpg;
import fromthemist.rpg.Talent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/** A winged thing with a venomous tail. Short sighted, but it comes to light from a long way off. */
public class StingerEntity extends Monster {
	private Vec3 lastCheck = Vec3.ZERO;

	public StingerEntity(final EntityType<? extends StingerEntity> type, final Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl<>(this, 20, true);
		this.xpReward = 15;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 8.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.FLYING_SPEED, 0.55)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 20.0);
	}

	@Override
	protected PathNavigation createNavigation(final Level level) {
		FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
		navigation.setCanFloat(true);
		return navigation;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void customServerAiStep(final ServerLevel level) {
		super.customServerAiStep(level);
		SkittererEntity.dissolveOutsideMist(this, level);
		if (this.tickCount % 20 == 0) {
			// Held up by a window with prey behind it: go through the glass.
			Vec3 here = this.position();
			boolean held = here.distanceToSqr(this.lastCheck) < 0.25;
			this.lastCheck = here;
			LivingEntity target = this.getTarget();
			if (held && target != null && level.getGameRules().get(GameRules.MOB_GRIEFING)) {
				BlockPos pos = HollowEntity.firstSolid(level, this.getEyePosition(), target.getEyePosition());
				if (pos != null && level.getBlockState(pos).getSoundType() == SoundType.GLASS) {
					level.destroyBlock(pos, false, this, 512);
				}
			}
		}

		if (this.tickCount % 40 == 0 && this.getTarget() == null) {
			// A torch in the hand or a lit window is a beacon.
			Player player = level.getNearestPlayer(this, 56.0);
			if (player != null && !player.isCreative() && !player.isSpectator()) {
				BlockPos at = player.blockPosition().above();
				if (level.getBrightness(LightLayer.BLOCK, at) >= 9) {
					this.setTarget(player);
				}
			}
		}
	}

	@Override
	public boolean doHurtTarget(final ServerLevel level, final Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living) {
			int poison = 100 - 50 * Rpg.rank(living, Talent.ANTIVENOM);
			if (poison > 0) {
				living.addEffect(new MobEffectInstance(MobEffects.POISON, poison, 0), this);
			}
		}

		return hit;
	}

	@Override
	protected void checkFallDamage(final double ya, final boolean onGround, final BlockState onState, final BlockPos pos) {
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
		return SoundEvents.BEE_LOOP_AGGRESSIVE;
	}

	@Override
	protected SoundEvent getHurtSound(final DamageSource source) {
		return SoundEvents.PHANTOM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PHANTOM_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.5F + this.random.nextFloat() * 0.15F;
	}

	@Override
	protected float getSoundVolume() {
		return 0.6F;
	}
}

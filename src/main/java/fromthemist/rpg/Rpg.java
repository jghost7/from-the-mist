package fromthemist.rpg;

import fromthemist.ModSounds;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import fromthemist.FromTheMist;
import fromthemist.MistState;
import fromthemist.ModContent;
import fromthemist.entity.HollowEntity;
import fromthemist.entity.SkittererEntity;
import fromthemist.entity.StingerEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Levels, talents and skills.
 * Level: experience from surviving nights and killing things; each level is one talent point.
 * Talents: see Talent. Skills: see Skill; they rise by use.
 * All the numbers a talent or skill is worth are in this file.
 */
public final class Rpg {
	private static final int[] COOLDOWNS = {3600, 4800, 3600};
	private static final int CLEAR_AIR_TICKS = 400;
	private static final Set<UUID> UNSENT = new HashSet<>();
	private static final Map<UUID, Vec3> LAST_POSITION = new HashMap<>();

	private Rpg() {
	}

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(RpgPayloads.Sync.TYPE, RpgPayloads.Sync.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RpgPayloads.Spend.TYPE, RpgPayloads.Spend.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RpgPayloads.UseAbility.TYPE, RpgPayloads.UseAbility.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(RpgPayloads.Spend.TYPE, (payload, context) -> spend(context.player(), payload.talent()));
		ServerPlayNetworking.registerGlobalReceiver(RpgPayloads.UseAbility.TYPE, (payload, context) -> useAbility(context.player(), payload.slot()));

		ServerPlayerEvents.JOIN.register(Rpg::refresh);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> refresh(newPlayer));

		// Skills rise by use: hitting, shooting, being hit.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, baseDamage, damageTaken, blocked) -> {
			if (damageTaken <= 0.0F) {
				return;
			}

			if (victim instanceof ServerPlayer player) {
				addSkillXp(player, Skill.ENDURANCE, damageTaken * 10.0F);
			}

			if (source.getEntity() instanceof ServerPlayer attacker && victim != attacker && victim instanceof Enemy) {
				addSkillXp(attacker, source.getDirectEntity() instanceof Projectile ? Skill.ARCHERY : Skill.COMBAT, Math.min(damageTaken, victim.getMaxHealth()) * 5.0F);
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
			if (!(source.getEntity() instanceof ServerPlayer killer)) {
				return;
			}

			boolean mist = victim instanceof SkittererEntity || victim instanceof StingerEntity;
			if (mist) {
				addXp(killer, 30);
				if (killer.getRandom().nextFloat() < 0.15F * progress(killer).rank(Talent.HARVESTER)) {
					Block.popResource(victim.level(), victim.blockPosition(), new ItemStack(ModContent.MIST_ESSENCE));
				}
			} else if (victim instanceof Enemy) {
				addXp(killer, 5);
			}
		});
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (player instanceof ServerPlayer serverPlayer) {
				float hardness = state.getDestroySpeed(level, pos);
				if (hardness > 0.0F) {
					addSkillXp(serverPlayer, Skill.MINING, Mth.clamp(hardness * 4.0F, 1.0F, 20.0F));
				}
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) {
				return;
			}

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				// A tenth of a point per block sprinted.
				Vec3 now = player.position();
				Vec3 before = LAST_POSITION.put(player.getUUID(), now);
				if (before != null && player.isSprinting() && !player.isPassenger()) {
					double moved = Math.sqrt(Mth.square(now.x - before.x) + Mth.square(now.z - before.z));
					if (moved > 1.0 && moved < 16.0) {
						addSkillXp(player, Skill.ATHLETICS, (float) moved);
					}
				}

				if (UNSENT.remove(player.getUUID())) {
					sync(player);
				}
			}
		});
	}

	// ------------------------------------------------------------------ data

	public static Progress progress(final ServerPlayer player) {
		MistState state = MistState.get(player.level().getServer().overworld());
		return state.players.computeIfAbsent(player.getUUID().toString(), id -> new Progress());
	}

	/** Rank of a talent for any player object; 0 on the client or for non-players. */
	public static int rank(final @Nullable Entity entity, final Talent talent) {
		return entity instanceof ServerPlayer player ? progress(player).rank(talent) : 0;
	}

	private static void changed(final ServerPlayer player) {
		MistState.get(player.level().getServer().overworld()).setDirty();
		UNSENT.add(player.getUUID());
	}

	private static void refresh(final ServerPlayer player) {
		applyAttributes(player);
		sync(player);
	}

	public static void sync(final ServerPlayer player) {
		Progress progress = progress(player);
		long time = player.level().getGameTime();
		int[] cooldowns = new int[Progress.ABILITIES];
		for (int i = 0; i < cooldowns.length; i++) {
			cooldowns[i] = (int) Math.max(0L, progress.readyAt[i] - time);
		}

		ServerPlayNetworking.send(player, new RpgPayloads.Sync(progress.level, progress.xp, progress.points, progress.talents.clone(),
			progress.skillLevels.clone(), progress.skillXp.clone(), cooldowns, (int) Math.max(0L, progress.clearAirUntil - time)));
	}

	// ------------------------------------------------------------------ experience

	public static void addXp(final ServerPlayer player, final int amount) {
		Progress progress = progress(player);
		if (progress.level >= Progress.MAX_LEVEL) {
			return;
		}

		progress.xp += Math.round(amount * (1.0F + 0.1F * progress.rank(Talent.QUICK_STUDY)));
		while (progress.level < Progress.MAX_LEVEL && progress.xp >= Progress.needed(progress.level)) {
			progress.xp -= Progress.needed(progress.level);
			progress.level++;
			progress.points++;
			player.sendSystemMessage(Component.translatable("message.fromthemist.level_up", progress.level).withStyle(ChatFormatting.GOLD));
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LEVEL_UP, SoundSource.PLAYERS, 1.0F, 1.0F);
		}

		changed(player);
	}

	/** Amount is in tenths of a point. */
	public static void addSkillXp(final ServerPlayer player, final Skill skill, final float amount) {
		Progress progress = progress(player);
		int index = skill.ordinal();
		if (progress.skillLevels[index] >= Skill.MAX_LEVEL) {
			return;
		}

		progress.skillXp[index] += Math.max(1, Math.round(amount * (1.0F + 0.1F * progress.rank(Talent.QUICK_STUDY))));
		boolean levelled = false;
		while (progress.skillLevels[index] < Skill.MAX_LEVEL && progress.skillXp[index] >= Skill.needed(progress.skillLevels[index])) {
			progress.skillXp[index] -= Skill.needed(progress.skillLevels[index]);
			progress.skillLevels[index]++;
			levelled = true;
			player.sendSystemMessage(Component.translatable("message.fromthemist.skill_up", Component.translatable("skill.fromthemist." + skill.key()), progress.skillLevels[index])
				.withStyle(ChatFormatting.AQUA));
		}

		if (levelled) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LEVEL_UP, SoundSource.PLAYERS, 0.6F, 1.5F);
			applyAttributes(player);
			addXp(player, 15);
		}

		changed(player);
	}

	// ------------------------------------------------------------------ talents

	public static void spend(final ServerPlayer player, final int index) {
		if (index < 0 || index >= Talent.VALUES.length) {
			return;
		}

		Progress progress = progress(player);
		Talent talent = Talent.VALUES[index];
		if (progress.canSpend(talent)) {
			progress.talents[index]++;
			progress.points--;
			applyAttributes(player);
			changed(player);
			sync(player);
		}
	}

	/** Passive bonuses that are plain numbers on the player are attribute modifiers, re-applied whenever anything changes. */
	public static void applyAttributes(final ServerPlayer player) {
		Progress progress = progress(player);
		modifier(player, Attributes.MAX_HEALTH, "vigor", 2.0 * progress.rank(Talent.VIGOR), AttributeModifier.Operation.ADD_VALUE);
		modifier(player, Attributes.ARMOR, "tough", 2.0 * progress.rank(Talent.TOUGH), AttributeModifier.Operation.ADD_VALUE);
		modifier(player, Attributes.MOVEMENT_SPEED, "speed",
			0.04 * progress.rank(Talent.FLEET) + 0.006 * progress.skillLevels[Skill.ATHLETICS.ordinal()], AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		modifier(player, Attributes.BLOCK_BREAK_SPEED, "mining", 0.02 * progress.skillLevels[Skill.MINING.ordinal()], AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
	}

	private static void modifier(final ServerPlayer player, final Holder<Attribute> attribute, final String name, final double amount, final AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}

		if (amount == 0.0) {
			instance.removeModifier(FromTheMist.id(name));
		} else {
			instance.addOrUpdateTransientModifier(new AttributeModifier(FromTheMist.id(name), amount, operation));
		}
	}

	/** Called for every hit on every living thing (see LivingEntityMixin); returns the damage to actually apply. */
	public static float modifyDamage(final LivingEntity victim, final DamageSource source, final float damage) {
		float result = damage;
		if (source.getEntity() instanceof ServerPlayer attacker && victim != attacker) {
			Progress progress = progress(attacker);
			Skill skill = source.getDirectEntity() instanceof Projectile ? Skill.ARCHERY : Skill.COMBAT;
			result *= 1.0F + 0.02F * progress.skillLevels[skill.ordinal()];
			if (victim instanceof SkittererEntity || victim instanceof StingerEntity) {
				result *= 1.0F + 0.1F * progress.rank(Talent.CULL);
			}
		}

		if (victim instanceof ServerPlayer player) {
			Progress progress = progress(player);
			result *= 1.0F - 0.01F * progress.skillLevels[Skill.ENDURANCE.ordinal()];
			if (source.getEntity() instanceof HollowEntity) {
				result *= 1.0F - 0.08F * progress.rank(Talent.THICK_SKIN);
			}
		}

		return result;
	}

	/** The nearest player this Hollow can sense. Quiet Step shortens the range for that player. */
	public static @Nullable ServerPlayer nearestSensed(final ServerLevel level, final Entity hollow, final double range) {
		ServerPlayer nearest = null;
		double best = Double.MAX_VALUE;
		for (ServerPlayer player : level.players()) {
			if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
				continue;
			}

			double reach = range * (1.0 - 0.15 * progress(player).rank(Talent.QUIET_STEP));
			double distance = player.distanceToSqr(hollow);
			if (distance <= reach * reach && distance < best) {
				best = distance;
				nearest = player;
			}
		}

		return nearest;
	}

	// ------------------------------------------------------------------ abilities

	public static void useAbility(final ServerPlayer player, final int slot) {
		if (slot < 0 || slot >= Progress.ABILITIES || !player.isAlive() || !(player.level() instanceof ServerLevel level)) {
			return;
		}

		Progress progress = progress(player);
		if (progress.rank(Talent.ability(slot)) <= 0) {
			return;
		}

		long time = level.getGameTime();
		if (time < progress.readyAt[slot]) {
			player.sendOverlayMessage(Component.translatable("message.fromthemist.ability_cooldown", (progress.readyAt[slot] - time) / 20L + 1L));
			return;
		}

		progress.readyAt[slot] = time + COOLDOWNS[slot];
		switch (slot) {
			case 0 -> {
				// Ward Pulse: every Hollow within 12 blocks is thrown back and slowed to a crawl for 8 seconds.
				for (HollowEntity hollow : level.getEntitiesOfClass(HollowEntity.class, player.getBoundingBox().inflate(12.0))) {
					Vec3 away = hollow.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
					Vec3 push = away.lengthSqr() < 0.01 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
					hollow.setDeltaMovement(push.x * 1.6, 0.5, push.z * 1.6);
					hollow.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 160, 4));
				}

				level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 80, 3.0, 0.6, 3.0, 0.15);
				level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WARD_PULSE, SoundSource.PLAYERS, 2.0F, 1.0F);
			}
			case 1 -> {
				// Clear Air: for 20 seconds the mist thins around you and its creatures within 32 blocks show through it.
				progress.clearAirUntil = time + CLEAR_AIR_TICKS;
				for (Monster monster : level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(32.0), m -> m instanceof SkittererEntity || m instanceof StingerEntity)) {
					monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, CLEAR_AIR_TICKS, 0));
				}

				level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CLEAR_AIR, SoundSource.PLAYERS, 1.5F, 1.0F);
			}
			default -> {
				// Adrenaline: 8 seconds of Speed II and Resistance I.
				player.addEffect(new MobEffectInstance(MobEffects.SPEED, 160, 1));
				player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 160, 0));
				level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ADRENALINE, SoundSource.PLAYERS, 1.5F, 1.0F);
			}
		}

		sync(player);
	}
}

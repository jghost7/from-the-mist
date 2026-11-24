package fromthemist;

import fromthemist.entity.HollowEntity;
import fromthemist.entity.LeviathanEntity;
import fromthemist.entity.SkittererEntity;
import fromthemist.entity.StingerEntity;
import fromthemist.rpg.Rpg;
import fromthemist.rpg.Talent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * Runs the two clocks of the mod on the overworld.
 * Night: the Hollow walk from dusk to dawn, every night, and get worse the more nights you survive.
 * Mist: every day or two a bank of mist forms a long way off and drifts across the world. You can get out of
 * its way, wait it out indoors, or go into it for what its creatures carry.
 */
public final class MistDirector {
	private static final int DAY = 24000;
	private static final int DUSK_WARNING = 11600;
	private static final int NIGHT_START = 13000;
	private static final int NIGHT_END = 23000;
	/** How far from a player a new bank forms, and how fast it drifts (blocks per tick). */
	private static final double FORM_DISTANCE = 300.0;
	private static final double DRIFT_SPEED = 0.03;
	private static final String[] COMPASS = {"east", "south_east", "south", "south_west", "west", "north_west", "north", "north_east"};

	private MistDirector() {
	}

	public static boolean isNight(final Level level) {
		long time = level.getDefaultClockTime() % DAY;
		return time >= NIGHT_START && time < NIGHT_END;
	}

	/** How thick the mist is where this entity stands, 0 to 1. */
	public static float intensityAt(final Entity entity) {
		if (entity.level() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
			return MistState.get(level).intensityAt(entity.getX(), entity.getZ());
		}

		return 0.0F;
	}

	/** How far the Hollow have escalated, 0 to MistState.MAX_STAGE. */
	public static int stage(final ServerLevel level) {
		return level.dimension() == Level.OVERWORLD ? MistState.get(level).stage() : 0;
	}

	public static void tick(final ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return;
		}

		MistState state = MistState.get(level);
		tickMist(level, state);
		tickDayNight(level, state);
		WardManager.tick(level, state);

		long time = level.getGameTime();
		if (time % 20L == 0L) {
			for (ServerPlayer player : level.players()) {
				ServerPlayNetworking.send(player, new MistPayload(state.intensityAt(player.getX(), player.getZ())));
			}
		}

		if (time % 200L == 0L && state.phase == MistState.MIST) {
			for (ServerPlayer player : level.players()) {
				double edge = state.edgeDistance(player.getX(), player.getZ());
				if (edge > 0.0 && edge < 200.0) {
					player.sendOverlayMessage(Component.translatable("message.fromthemist.mist_near", Math.max(10, (int) (edge / 10.0) * 10), direction(state, player)));
				}
			}
		}

		if (time % 100L == 0L && level.getDifficulty() != Difficulty.PEACEFUL) {
			spawn(level, state);
		}
	}

	// ------------------------------------------------------------------ the bank of mist

	private static void tickMist(final ServerLevel level, final MistState state) {
		if (level.players().isEmpty()) {
			return;
		}

		state.phaseTicks--;
		if (state.phase != MistState.CLEAR) {
			state.centerX += state.velX;
			state.centerZ += state.velZ;
		}

		state.setDirty();
		if (state.phaseTicks > 0) {
			return;
		}

		switch (state.phase) {
			case MistState.CLEAR -> startMist(level, state, null, false);
			case MistState.MIST -> endMist(level, state);
			default -> {
				state.phase = MistState.CLEAR;
				state.phaseTicks = 24000 + level.getRandom().nextInt(24000);
			}
		}
	}

	/**
	 * Forms a new bank. Normally it forms far from a player and drifts roughly toward where they are now,
	 * so there are minutes of warning and it can miss; onTop drops it straight onto them instead.
	 */
	public static void startMist(final ServerLevel level, final MistState state, final @Nullable ServerPlayer focus, final boolean onTop) {
		RandomSource random = level.getRandom();
		ServerPlayer player = focus != null ? focus : level.players().isEmpty() ? null : level.players().get(random.nextInt(level.players().size()));
		double x = player == null ? 0.0 : player.getX();
		double z = player == null ? 0.0 : player.getZ();
		float bearing = random.nextFloat() * Mth.TWO_PI;
		double speed = DRIFT_SPEED + random.nextDouble() * 0.015;
		if (onTop) {
			state.centerX = x;
			state.centerZ = z;
			state.velX = Mth.cos(bearing) * DRIFT_SPEED * 0.5;
			state.velZ = Mth.sin(bearing) * DRIFT_SPEED * 0.5;
		} else {
			state.centerX = x + Mth.cos(bearing) * FORM_DISTANCE;
			state.centerZ = z + Mth.sin(bearing) * FORM_DISTANCE;
			// Aim at a point up to 70 blocks off the player, so it sometimes only clips them or passes by.
			double aimX = x + (random.nextDouble() - 0.5) * 140.0 - state.centerX;
			double aimZ = z + (random.nextDouble() - 0.5) * 140.0 - state.centerZ;
			double length = Math.max(1.0, Math.sqrt(aimX * aimX + aimZ * aimZ));
			state.velX = aimX / length * speed;
			state.velZ = aimZ / length * speed;
		}

		state.phase = MistState.MIST;
		state.phaseTicks = 16000 + random.nextInt(6000);
		state.mistsSeen++;
		state.setDirty();
		for (ServerPlayer listener : level.players()) {
			Component message = state.edgeDistance(listener.getX(), listener.getZ()) < 0.0
				? Component.translatable("message.fromthemist.mist_here")
				: Component.translatable("message.fromthemist.mist_coming", direction(state, listener));
			tell(level, listener, message, SoundEvents.RAID_HORN.value(), 64.0F, 0.6F);
		}
	}

	public static void endMist(final ServerLevel level, final MistState state) {
		state.phase = MistState.RECEDING;
		state.phaseTicks = MistState.FADE_TICKS;
		state.setDirty();
		for (ServerPlayer listener : level.players()) {
			if (state.edgeDistance(listener.getX(), listener.getZ()) < 60.0) {
				tell(level, listener, Component.translatable("message.fromthemist.mist_leaving"), SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 0.5F);
			}
		}
	}

	/** Compass direction from the player to the middle of the bank, as a translated word. */
	public static Component direction(final MistState state, final ServerPlayer player) {
		double angle = Mth.atan2(state.centerZ - player.getZ(), state.centerX - player.getX());
		int index = Math.floorMod(Math.round((float) (angle / (Math.PI / 4.0))), 8);
		return Component.translatable("direction.fromthemist." + COMPASS[index]);
	}

	// ------------------------------------------------------------------ day and night

	private static void tickDayNight(final ServerLevel level, final MistState state) {
		long clock = level.getDefaultClockTime();
		int timeOfDay = (int) (clock % DAY);

		// Counts dusks and dawns passed, so one skipped by sleeping or /time still registers once.
		long dusk = (clock + DAY - DUSK_WARNING) / DAY;
		if (state.lastDusk < 0L) {
			state.lastDusk = dusk;
		} else if (dusk != state.lastDusk) {
			state.lastDusk = dusk;
			if (timeOfDay >= DUSK_WARNING && timeOfDay < NIGHT_START) {
				for (ServerPlayer player : level.players()) {
					tell(level, player, Component.translatable("message.fromthemist.dusk"), SoundEvents.BELL_BLOCK, 3.0F, 0.6F);
				}
			}
		}

		long dawn = (clock + DAY - NIGHT_END) / DAY;
		if (state.lastDawn < 0L) {
			state.lastDawn = dawn;
		} else if (dawn != state.lastDawn) {
			state.lastDawn = dawn;
			int before = state.stage();
			state.nights++;
			wearTalismans(level, state);
			for (ServerPlayer player : level.players()) {
				player.sendSystemMessage(Component.translatable("message.fromthemist.dawn", state.nights).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
				Rpg.addXp(player, 100 + 25 * state.stage()); // living through a night is the main source of levels
				if (state.stage() > before) {
					player.sendSystemMessage(Component.translatable("message.fromthemist.escalate_" + state.stage()).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
				}
			}
		}

		state.setDirty();
	}

	/** Every dawn each hanging talisman has one night less in it. */
	private static void wearTalismans(final ServerLevel level, final MistState state) {
		for (long key : state.talismans) {
			BlockPos pos = BlockPos.of(key);
			if (!level.isLoaded(pos)) {
				continue;
			}

			BlockState block = level.getBlockState(pos);
			if (!block.is(ModContent.TALISMAN)) {
				continue;
			}

			int charge = block.getValue(TalismanBlock.CHARGE);
			if (charge <= 0) {
				continue;
			}

			float keep = 0.0F;
			for (ServerPlayer player : level.players()) {
				if (player.blockPosition().closerThan(pos, 24.0)) {
					keep = Math.max(keep, 0.15F * Rpg.rank(player, Talent.KEEPER));
				}
			}

			if (level.getRandom().nextFloat() < keep) {
				continue; // a Keeper nearby held the night for free
			}

			level.setBlock(pos, block.setValue(TalismanBlock.CHARGE, charge - 1), Block.UPDATE_ALL);
			String message = charge - 1 <= 0 ? "message.fromthemist.talisman_spent" : charge - 1 == 1 ? "message.fromthemist.talisman_last_night" : null;
			if (message != null) {
				level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 0.5F);
				for (ServerPlayer player : level.players()) {
					if (player.blockPosition().closerThan(pos, 24.0)) {
						player.sendSystemMessage(Component.translatable(message).withStyle(ChatFormatting.RED));
					}
				}
			}
		}
	}

	private static void tell(final ServerLevel level, final ServerPlayer player, final Component message, final SoundEvent sound, final float volume, final float pitch) {
		player.sendSystemMessage(message.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		player.sendOverlayMessage(message);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.AMBIENT, volume, pitch);
	}

	// ------------------------------------------------------------------ spawning

	private static void spawn(final ServerLevel level, final MistState state) {
		RandomSource random = level.getRandom();
		boolean night = isNight(level);
		int stage = state.stage();
		for (ServerPlayer player : level.players()) {
			float mist = state.intensityAt(player.getX(), player.getZ());
			if (player.isSpectator() || !night && mist < 0.5F) {
				continue;
			}

			BlockPos at = player.blockPosition();
			int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
			if (at.getY() < surface - 14) {
				continue; // deep underground: neither the night nor the mist reaches down here
			}

			if (night) {
				// More of them, and from closer in, the longer you have lasted.
				int cap = 3 + stage * 2;
				if (count(level, player, HollowEntity.class, 96.0) < cap) {
					spawnNear(level, state, ModContent.HOLLOW, player, 26 - stage * 3, 46 - stage * 4, 0, false);
				}
			}

			if (mist >= 0.5F) {
				if (count(level, player, SkittererEntity.class, 64.0) < 6) {
					int pack = 1 + random.nextInt(2);
					for (int i = 0; i < pack; i++) {
						spawnNear(level, state, ModContent.SKITTERER, player, 18, 38, 0, true);
					}
				}

				if (count(level, player, StingerEntity.class, 64.0) < 4 && random.nextInt(2) == 0) {
					spawnNear(level, state, ModContent.STINGER, player, 18, 38, 5 + random.nextInt(6), true);
				}

				if (mist >= 0.95F && random.nextInt(10) == 0 && count(level, player, LeviathanEntity.class, 220.0) == 0) {
					// One in three is the heavy kind.
					spawnLeviathan(level, player, random.nextInt(3) == 0 ? ModContent.COLOSSUS : ModContent.LEVIATHAN);
				}
			}
		}
	}

	private static <T extends Mob> int count(final ServerLevel level, final ServerPlayer player, final Class<T> type, final double range) {
		return level.getEntitiesOfClass(type, player.getBoundingBox().inflate(range)).size();
	}

	private static <T extends Mob> void spawnNear(
		final ServerLevel level, final MistState state, final EntityType<T> type, final ServerPlayer player, final int min, final int max, final int lift, final boolean inMist
	) {
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < 10; attempt++) {
			float angle = random.nextFloat() * Mth.TWO_PI;
			int distance = min + random.nextInt(max - min + 1);
			int x = Mth.floor(player.getX() + Mth.cos(angle) * distance);
			int z = Mth.floor(player.getZ() + Mth.sin(angle) * distance);
			if (!level.isLoaded(new BlockPos(x, player.getBlockY(), z)) || inMist && state.intensityAt(x, z) < 0.3F) {
				continue;
			}

			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos pos = new BlockPos(x, y + lift, z);
			if (!level.getFluidState(pos.below()).isEmpty() && lift == 0) {
				continue;
			}

			if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
				continue;
			}

			if (WardManager.isWarded(level, pos)) {
				continue;
			}

			T mob = type.create(level, EntitySpawnReason.EVENT);
			if (mob == null) {
				return;
			}

			mob.snapTo(x + 0.5, pos.getY(), z + 0.5, random.nextFloat() * 360.0F, 0.0F);
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
			level.addFreshEntity(mob);
			return;
		}
	}

	/** Starts a leviathan well outside the mist's edge of sight, on a line that passes close by the player. */
	public static void spawnLeviathan(final ServerLevel level, final ServerPlayer player, final EntityType<? extends LeviathanEntity> type) {
		RandomSource random = level.getRandom();
		float angle = random.nextFloat() * Mth.TWO_PI;
		double x = player.getX() + Mth.cos(angle) * 80.0;
		double z = player.getZ() + Mth.sin(angle) * 80.0;
		BlockPos start = BlockPos.containing(x, player.getY(), z);
		if (!level.isLoaded(start)) {
			return;
		}

		LeviathanEntity leviathan = type.create(level, EntitySpawnReason.EVENT);
		if (leviathan == null) {
			return;
		}

		// Aim for a point 8 to 20 blocks to one side of the player.
		float side = angle + Mth.HALF_PI * (random.nextBoolean() ? 1.0F : -1.0F);
		double miss = 8.0 + random.nextInt(13);
		double aimX = player.getX() + Mth.cos(side) * miss;
		double aimZ = player.getZ() + Mth.sin(side) * miss;
		float heading = (float) (Mth.atan2(aimZ - z, aimX - x) * Mth.RAD_TO_DEG) - 90.0F;
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
		leviathan.snapTo(x, y, z, heading, 0.0F);
		leviathan.setHeading(heading);
		level.addFreshEntity(leviathan);
	}

	// ------------------------------------------------------------------ players

	public static void onJoin(final ServerPlayer player) {
		ServerPlayNetworking.send(player, new MistPayload(intensityAt(player)));
		if (player.addTag("fromthemist.started")) {
			if (!player.addItem(ModContent.talisman(3))) {
				Block.popResource(player.level(), player.blockPosition(), ModContent.talisman(3));
			}

			player.sendSystemMessage(Component.translatable("message.fromthemist.welcome").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		}
	}
}

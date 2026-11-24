package fromthemist;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.util.EventResult;
import fromthemist.rpg.Rpg;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FromTheMist implements ModInitializer {
	public static final String MOD_ID = "fromthemist";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModContent.init();
		ModSounds.init();
		Rpg.init();
		PayloadTypeRegistry.clientboundPlay().register(MistPayload.TYPE, MistPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ShakePayload.TYPE, ShakePayload.STREAM_CODEC);

		ServerTickEvents.END_LEVEL_TICK.register(MistDirector::tick);
		ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> MistDirector.onJoin(listener.player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				DeathRules.onRespawn(newPlayer);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> WardManager.clear());
		ServerLifecycleEvents.SERVER_STARTING.register(server -> WardManager.clear());

		// You only sleep behind a sealed ward, and never with the mist over you.
		EntitySleepEvents.ALLOW_SLEEPING.register((player, pos) -> {
			Level level = player.level();
			if (level.isClientSide() || level.dimension() != Level.OVERWORLD) {
				return null;
			}

			if (MistDirector.intensityAt(player) > 0.3F) {
				return new Player.BedSleepingProblem(Component.translatable("message.fromthemist.sleep_mist"));
			}

			if (!WardManager.isWarded(level, pos)) {
				return new Player.BedSleepingProblem(Component.translatable("message.fromthemist.sleep_unwarded"));
			}

			return null;
		});
		// The Hollow standing at the window do not stop you sleeping once the ward holds.
		EntitySleepEvents.ALLOW_NEARBY_MONSTERS.register((player, pos, vanillaResult) ->
			!player.level().isClientSide() && WardManager.isWarded(player.level(), pos) ? EventResult.ALLOW : EventResult.PASS);

		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(
			Commands.literal("mist").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("start").executes(c -> mistCommand(c, "start")))
				.then(Commands.literal("approach").executes(c -> mistCommand(c, "approach")))
				.then(Commands.literal("stop").executes(c -> mistCommand(c, "stop")))
				.then(Commands.literal("leviathan").executes(c -> mistCommand(c, "leviathan")))
				.then(Commands.literal("colossus").executes(c -> mistCommand(c, "colossus")))
				.then(Commands.literal("status").executes(c -> mistCommand(c, "status")))
				.then(Commands.literal("xp").then(Commands.argument("amount", IntegerArgumentType.integer(1, 100000)).executes(c -> {
					ServerPlayer player = c.getSource().getPlayer();
					if (player != null) {
						Rpg.addXp(player, IntegerArgumentType.getInteger(c, "amount"));
					}

					return 1;
				})))
				.then(Commands.literal("nights").then(Commands.argument("count", IntegerArgumentType.integer(0, 1000)).executes(c -> {
					MistState state = MistState.get(c.getSource().getServer().overworld());
					state.nights = IntegerArgumentType.getInteger(c, "count");
					state.setDirty();
					return mistCommand(c, "status");
				})))
		));
	}

	private static int mistCommand(final CommandContext<CommandSourceStack> context, final String action) {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getServer().overworld();
		MistState state = MistState.get(level);
		ServerPlayer player = source.getPlayer();
		switch (action) {
			case "start" -> MistDirector.startMist(level, state, player, true);
			case "approach" -> MistDirector.startMist(level, state, player, false);
			case "stop" -> {
				if (state.phase == MistState.MIST) {
					MistDirector.endMist(level, state);
					state.phaseTicks = 200;
				}
			}
			case "leviathan", "colossus" -> {
				if (player != null) {
					MistDirector.spawnLeviathan(level, player, action.equals("colossus") ? ModContent.COLOSSUS : ModContent.LEVIATHAN);
				}
			}
			default -> {
			}
		}

		String mist = switch (state.phase) {
			case MistState.MIST -> "bank at " + (int) state.centerX + ", " + (int) state.centerZ + " for another " + state.phaseTicks / 20 + "s";
			case MistState.RECEDING -> "thinning out";
			default -> "clear, next bank in " + state.phaseTicks / 20 + "s";
		};
		String where = player == null || state.phase == MistState.CLEAR ? "" : state.edgeDistance(player.getX(), player.getZ()) < 0.0
			? " (you are in it)"
			: " (edge " + (int) state.edgeDistance(player.getX(), player.getZ()) + " blocks away)";
		source.sendSuccess(() -> Component.literal("Mist: " + mist + where + ". Nights survived: " + state.nights + ", Hollow stage " + state.stage() + " of " + MistState.MAX_STAGE
			+ ". Talismans tracked: " + state.talismans.size() + "."), false);
		return 1;
	}

	public static Identifier id(final String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}

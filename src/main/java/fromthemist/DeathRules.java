package fromthemist;

import fromthemist.rpg.Progress;
import fromthemist.rpg.Rpg;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * What dying costs.
 * Standard worlds: you get back up with what was on you (hotbar, armour, off hand); what was in your pack
 * stays where you fell, glowing, and never despawns, so going back for it is a choice rather than a race.
 * Hardcore worlds (Minecraft's own Hardcore option at world creation): one life. The mod only adds the epitaph.
 * Level, talents and skills are kept in both; with keepInventory on, vanilla keeps everything and this stands aside.
 */
public final class DeathRules {
	private static final int HOTBAR = 9;
	private static final int PACK_END = Inventory.INVENTORY_SIZE;
	private static final int WORN_END = Inventory.SLOT_OFFHAND + 1;

	private DeathRules() {
	}

	/** Called at the very start of a player's death, before vanilla drops anything (see ServerPlayerMixin). */
	public static void onDeath(final ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		MinecraftServer server = level.getServer();
		MistState state = MistState.get(server.overworld());
		Progress progress = Rpg.progress(player);
		if (server.isHardcore()) {
			server.getPlayerList().broadcastSystemMessage(
				Component.translatable("message.fromthemist.run_over", player.getDisplayName(), state.nights, progress.level).withStyle(ChatFormatting.DARK_RED), false);
			return;
		}

		if (level.getGameRules().get(GameRules.KEEP_INVENTORY) || player.isSpectator()) {
			return;
		}

		Inventory inventory = player.getInventory();
		progress.keptSlots.clear();
		progress.keptItems.clear();
		for (int slot = 0; slot < WORN_END; slot++) {
			if (slot >= HOTBAR && slot < PACK_END) {
				continue;
			}

			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty()) {
				progress.keptSlots.add(slot);
				progress.keptItems.add(stack.copy());
				inventory.setItem(slot, ItemStack.EMPTY);
			}
		}

		boolean dropped = false;
		for (int slot = HOTBAR; slot < PACK_END; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}

			ItemEntity item = new ItemEntity(level, player.getX() + (player.getRandom().nextDouble() - 0.5), player.getY() + 0.5, player.getZ() + (player.getRandom().nextDouble() - 0.5), stack.copy());
			item.setUnlimitedLifetime();
			item.setDefaultPickUpDelay();
			item.setGlowingTag(true);
			level.addFreshEntity(item);
			inventory.setItem(slot, ItemStack.EMPTY);
			dropped = true;
		}

		if (dropped) {
			BlockPos at = player.blockPosition();
			player.sendSystemMessage(Component.translatable("message.fromthemist.pack_dropped", at.getX(), at.getY(), at.getZ()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		}

		state.setDirty();
	}

	/** Hands back what the player was carrying on them when they died. */
	public static void onRespawn(final ServerPlayer player) {
		Progress progress = Rpg.progress(player);
		if (progress.keptItems.isEmpty()) {
			return;
		}

		Inventory inventory = player.getInventory();
		for (int i = 0; i < progress.keptItems.size(); i++) {
			int slot = i < progress.keptSlots.size() ? progress.keptSlots.get(i) : -1;
			ItemStack stack = progress.keptItems.get(i);
			if (slot >= 0 && inventory.getItem(slot).isEmpty()) {
				inventory.setItem(slot, stack);
			} else if (!inventory.add(stack)) {
				net.minecraft.world.level.block.Block.popResource(player.level(), player.blockPosition(), stack);
			}
		}

		progress.keptSlots.clear();
		progress.keptItems.clear();
		MistState.get(player.level().getServer().overworld()).setDirty();
	}
}

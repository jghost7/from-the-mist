package fromthemist.client;

import com.mojang.blaze3d.platform.InputConstants;
import fromthemist.FromTheMist;
import fromthemist.rpg.Progress;
import fromthemist.rpg.RpgPayloads;
import fromthemist.rpg.Talent;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** What this client knows about its own character, plus the keys that open the screen and fire abilities. */
public final class RpgClient {
	/** The latest copy from the server. Cooldowns here count down locally between updates. */
	public static final Progress PROGRESS = new Progress();
	public static final int[] COOLDOWNS = new int[Progress.ABILITIES];
	public static int clearAir;
	/** Bumped on every update so an open screen knows to redraw its buttons. */
	public static int version;

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(FromTheMist.id("main"));
	public static final KeyMapping OPEN = new KeyMapping("key.fromthemist.character", InputConstants.KEY_K, CATEGORY);
	public static final KeyMapping[] ABILITY = {
		new KeyMapping("key.fromthemist.ability_warden", InputConstants.KEY_Z, CATEGORY),
		new KeyMapping("key.fromthemist.ability_hunter", InputConstants.KEY_X, CATEGORY),
		new KeyMapping("key.fromthemist.ability_survivor", InputConstants.KEY_C, CATEGORY)
	};

	private RpgClient() {
	}

	public static void init() {
		KeyMappingHelper.registerKeyMapping(OPEN);
		for (KeyMapping key : ABILITY) {
			KeyMappingHelper.registerKeyMapping(key);
		}

		ClientPlayNetworking.registerGlobalReceiver(RpgPayloads.Sync.TYPE, (payload, context) -> {
			PROGRESS.level = payload.level();
			PROGRESS.xp = payload.xp();
			PROGRESS.points = payload.points();
			copy(payload.talents(), PROGRESS.talents);
			copy(payload.skillLevels(), PROGRESS.skillLevels);
			copy(payload.skillXp(), PROGRESS.skillXp);
			copy(payload.cooldowns(), COOLDOWNS);
			clearAir = payload.clearAir();
			version++;
		});
	}

	public static void tick(final Minecraft client) {
		if (client.player == null || client.isPaused()) {
			return;
		}

		for (int i = 0; i < COOLDOWNS.length; i++) {
			COOLDOWNS[i] = Math.max(0, COOLDOWNS[i] - 1);
		}

		clearAir = Math.max(0, clearAir - 1);
		while (OPEN.consumeClick()) {
			client.gui.setScreen(new CharacterScreen());
		}

		for (int slot = 0; slot < ABILITY.length; slot++) {
			while (ABILITY[slot].consumeClick()) {
				if (PROGRESS.rank(Talent.ability(slot)) > 0) {
					ClientPlayNetworking.send(new RpgPayloads.UseAbility(slot));
				}
			}
		}
	}

	/** How much further than normal this player sees in the mist. */
	public static float mistSight() {
		return (1.0F + 0.25F * PROGRESS.rank(Talent.MIST_EYES)) * (clearAir > 0 ? 2.5F : 1.0F);
	}

	private static void copy(final int[] from, final int[] to) {
		System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
	}
}

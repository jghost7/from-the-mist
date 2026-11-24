package fromthemist;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * The mod's own sounds. The files and sounds.json are built by tools/make_sounds.py: some are synthesized there,
 * some are CC0 recordings (SOUND-CREDITS.md).
 */
public final class ModSounds {
	public static final SoundEvent MIST_LOOP = register("mist.loop");
	public static final SoundEvent MIST_HORN = register("mist.horn");
	public static final SoundEvent DUSK_BELL = register("dusk.bell");
	public static final SoundEvent HOLLOW_IDLE = register("hollow.idle");
	public static final SoundEvent HOLLOW_KNOCK = register("hollow.knock");
	public static final SoundEvent SKITTERER_IDLE = register("skitterer.idle");
	public static final SoundEvent SKITTERER_HURT = register("skitterer.hurt");
	public static final SoundEvent STINGER_IDLE = register("stinger.idle");
	public static final SoundEvent STINGER_HURT = register("stinger.hurt");
	public static final SoundEvent LEVIATHAN_CALL = register("leviathan.call");
	public static final SoundEvent COLOSSUS_CALL = register("colossus.call");
	public static final SoundEvent LEVIATHAN_STEP = register("leviathan.step");
	public static final SoundEvent LEVIATHAN_CRACK = register("leviathan.crack");
	public static final SoundEvent LEVIATHAN_CRASH = register("leviathan.crash");
	public static final SoundEvent TALISMAN_FEED = register("talisman.feed");
	public static final SoundEvent TALISMAN_CRACK = register("talisman.crack");
	public static final SoundEvent WARD_PULSE = register("ability.ward_pulse");
	public static final SoundEvent CLEAR_AIR = register("ability.clear_air");
	public static final SoundEvent ADRENALINE = register("ability.adrenaline");
	public static final SoundEvent LEVEL_UP = register("level_up");

	private ModSounds() {
	}

	private static SoundEvent register(final String name) {
		Identifier id = FromTheMist.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {
		// Loading the class registers everything above.
	}
}

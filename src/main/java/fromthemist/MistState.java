package fromthemist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fromthemist.rpg.Progress;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Everything the mod remembers about a world: where the bank of mist is and where it is heading,
 * how many nights have been survived, where talismans hang, and every player's progression.
 */
public class MistState extends SavedData {
	public static final int CLEAR = 0;
	public static final int MIST = 2;
	public static final int RECEDING = 3;
	/** Radius of the bank in blocks, and how wide its thinning edge is. */
	public static final float RADIUS = 96.0F;
	public static final float EDGE = 48.0F;
	/** Ticks the bank takes to shrink away at the end. */
	public static final int FADE_TICKS = 1200;
	/** The Hollow step up every this many nights, to a top stage of MAX_STAGE. */
	public static final int NIGHTS_PER_STAGE = 3;
	public static final int MAX_STAGE = 4;

	public static final Codec<MistState> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.fieldOf("phase").forGetter(s -> s.phase),
		Codec.INT.fieldOf("phase_ticks").forGetter(s -> s.phaseTicks),
		Codec.LONG.fieldOf("last_dawn").forGetter(s -> s.lastDawn),
		Codec.LONG.fieldOf("last_dusk").forGetter(s -> s.lastDusk),
		Codec.INT.fieldOf("mists_seen").forGetter(s -> s.mistsSeen),
		Codec.INT.optionalFieldOf("nights", 0).forGetter(s -> s.nights),
		Codec.DOUBLE.optionalFieldOf("center_x", 0.0).forGetter(s -> s.centerX),
		Codec.DOUBLE.optionalFieldOf("center_z", 0.0).forGetter(s -> s.centerZ),
		Codec.DOUBLE.optionalFieldOf("vel_x", 0.0).forGetter(s -> s.velX),
		Codec.DOUBLE.optionalFieldOf("vel_z", 0.0).forGetter(s -> s.velZ),
		Codec.LONG.listOf().fieldOf("talismans").forGetter(s -> s.talismans),
		Codec.unboundedMap(Codec.STRING, Progress.CODEC).optionalFieldOf("players", Map.of()).forGetter(s -> s.players)
	).apply(i, MistState::new));

	public static final SavedDataType<MistState> TYPE = new SavedDataType<>(
		FromTheMist.id("state"), MistState::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	public int phase = CLEAR;
	/** Ticks left in the current phase. A new world gets about a day and a half of clear air first. */
	public int phaseTicks = 34000;
	public long lastDawn = -1L;
	public long lastDusk = -1L;
	public int mistsSeen = 0;
	/** Dawns seen. Drives how aggressive the Hollow are. */
	public int nights = 0;
	public double centerX;
	public double centerZ;
	/** Drift in blocks per tick. */
	public double velX;
	public double velZ;
	public final List<Long> talismans = new ArrayList<>();
	/** Each player's level, talents and skills, by player id. */
	public final Map<String, Progress> players = new HashMap<>();

	public MistState() {
	}

	private MistState(
		final int phase, final int phaseTicks, final long lastDawn, final long lastDusk, final int mistsSeen, final int nights,
		final double centerX, final double centerZ, final double velX, final double velZ, final List<Long> talismans, final Map<String, Progress> players
	) {
		this.phase = phase == MIST || phase == RECEDING ? phase : CLEAR;
		this.phaseTicks = phaseTicks;
		this.lastDawn = lastDawn;
		this.lastDusk = lastDusk;
		this.mistsSeen = mistsSeen;
		this.nights = nights;
		this.centerX = centerX;
		this.centerZ = centerZ;
		this.velX = velX;
		this.velZ = velZ;
		this.talismans.addAll(talismans);
		this.players.putAll(players);
	}

	public static MistState get(final ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public float radius() {
		return switch (this.phase) {
			case MIST -> RADIUS;
			case RECEDING -> RADIUS * Math.clamp(this.phaseTicks / (float) FADE_TICKS, 0.0F, 1.0F);
			default -> 0.0F;
		};
	}

	/** Blocks from this point to the edge of the bank; negative inside it. */
	public double edgeDistance(final double x, final double z) {
		return Math.sqrt(Mth.square(x - this.centerX) + Mth.square(z - this.centerZ)) - this.radius();
	}

	/** How thick the mist is at this point: 0 outside the bank, rising to 1 well inside it. */
	public float intensityAt(final double x, final double z) {
		if (this.radius() <= 0.0F) {
			return 0.0F;
		}

		return (float) Math.clamp(-this.edgeDistance(x, z) / EDGE, 0.0, 1.0);
	}

	/** 0 on the first nights, up to MAX_STAGE. */
	public int stage() {
		return Math.min(MAX_STAGE, this.nights / NIGHTS_PER_STAGE);
	}

	public void addTalisman(final BlockPos pos) {
		long key = pos.asLong();
		if (!this.talismans.contains(key)) {
			this.talismans.add(key);
			this.setDirty();
		}
	}
}

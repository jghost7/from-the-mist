package fromthemist.rpg;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** One player's level, talents and skills. Lives in the world save, keyed by player id. */
public class Progress {
	public static final int MAX_LEVEL = 30;
	public static final int ABILITIES = 3;

	public static final Codec<Progress> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.fieldOf("level").forGetter(p -> p.level),
		Codec.INT.fieldOf("xp").forGetter(p -> p.xp),
		Codec.INT.fieldOf("points").forGetter(p -> p.points),
		Codec.INT.listOf().fieldOf("talents").forGetter(p -> list(p.talents)),
		Codec.INT.listOf().fieldOf("skill_levels").forGetter(p -> list(p.skillLevels)),
		Codec.INT.listOf().fieldOf("skill_xp").forGetter(p -> list(p.skillXp))
	).apply(i, Progress::new));

	public int level;
	public int xp;
	/** Unspent talent points. */
	public int points;
	public final int[] talents = new int[Talent.VALUES.length];
	public final int[] skillLevels = new int[Skill.VALUES.length];
	/** In tenths; see Skill.needed. */
	public final int[] skillXp = new int[Skill.VALUES.length];
	/** Game time at which each ability can be used again. Not saved: cooldowns reset when the world reloads. */
	public final long[] readyAt = new long[ABILITIES];
	/** Game time until which Clear Air is thinning the mist for this player. */
	public long clearAirUntil;

	public Progress() {
	}

	private Progress(final int level, final int xp, final int points, final List<Integer> talents, final List<Integer> skillLevels, final List<Integer> skillXp) {
		this.level = level;
		this.xp = xp;
		this.points = points;
		copy(talents, this.talents);
		copy(skillLevels, this.skillLevels);
		copy(skillXp, this.skillXp);
	}

	/** Experience needed to go from this level to the next. */
	public static int needed(final int level) {
		return 100 + 40 * level;
	}

	public int rank(final Talent talent) {
		return this.talents[talent.ordinal()];
	}

	public int spentIn(final Talent.Tree tree) {
		int spent = 0;
		for (Talent talent : Talent.VALUES) {
			if (talent.tree == tree) {
				spent += this.talents[talent.ordinal()];
			}
		}

		return spent;
	}

	/** Whether a point can go into this talent right now. Shared by the server check and the screen. */
	public boolean canSpend(final Talent talent) {
		if (this.points <= 0 || this.rank(talent) >= talent.maxRank) {
			return false;
		}

		if (talent.position() > 0 && this.talents[talent.ordinal() - 1] <= 0) {
			return false;
		}

		return !talent.isAbility() || this.spentIn(talent.tree) >= Talent.CAPSTONE_POINTS;
	}

	private static List<Integer> list(final int[] values) {
		List<Integer> list = new ArrayList<>(values.length);
		for (int value : values) {
			list.add(value);
		}

		return list;
	}

	private static void copy(final List<Integer> from, final int[] to) {
		for (int i = 0; i < Math.min(from.size(), to.length); i++) {
			to[i] = from.get(i);
		}
	}
}

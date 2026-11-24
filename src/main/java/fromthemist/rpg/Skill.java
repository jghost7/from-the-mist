package fromthemist.rpg;

/** Skills that rise by doing the thing. Order matters: the ordinal is what is saved and sent. */
public enum Skill {
	COMBAT,     // melee damage dealt -> 2% more melee damage per level
	ARCHERY,    // projectile damage dealt -> 2% more projectile damage per level
	MINING,     // blocks broken -> 2% faster digging per level
	ATHLETICS,  // distance sprinted -> 0.6% faster on foot per level
	ENDURANCE;  // damage taken -> 1% less damage taken per level

	public static final Skill[] VALUES = values();
	public static final int MAX_LEVEL = 25;

	/** Skill experience is kept in tenths so small actions still count. */
	public static int needed(final int level) {
		return (40 + 25 * level) * 10;
	}

	public String key() {
		return this.name().toLowerCase(java.util.Locale.ROOT);
	}
}

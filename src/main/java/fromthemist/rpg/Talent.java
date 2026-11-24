package fromthemist.rpg;

/**
 * The talent trees. Each tree is five talents in a row: every talent needs at least one rank in the one before it,
 * and the last one is an active ability that also needs CAPSTONE_POINTS spent in its tree.
 * Order matters: the ordinal is what is saved and sent over the network.
 */
public enum Talent {
	// Warden: the night and the talisman.
	STEADY_HANDS(Tree.WARDEN, 3),   // each essence feeds a talisman one more night per rank
	QUIET_STEP(Tree.WARDEN, 3),     // the Hollow sense you from 15% less far per rank
	THICK_SKIN(Tree.WARDEN, 3),     // 8% less damage from the Hollow per rank
	KEEPER(Tree.WARDEN, 2),         // 15% chance per rank that a talisman near you loses nothing at dawn
	WARD_PULSE(Tree.WARDEN, 1),     // ability: throw back and slow every Hollow near you

	// Hunter: the mist.
	MIST_EYES(Tree.HUNTER, 3),      // see 25% further in the mist per rank
	CULL(Tree.HUNTER, 3),           // 10% more damage to mist creatures per rank
	HARVESTER(Tree.HUNTER, 3),      // 15% chance per rank of an extra essence from a mist kill
	ANTIVENOM(Tree.HUNTER, 2),      // stinger poison lasts half as long; immune at rank 2
	CLEAR_AIR(Tree.HUNTER, 1),      // ability: thin the mist around you and mark its creatures

	// Survivor: staying alive.
	VIGOR(Tree.SURVIVOR, 3),        // one more heart per rank
	FLEET(Tree.SURVIVOR, 3),        // 4% faster on foot per rank
	QUICK_STUDY(Tree.SURVIVOR, 3),  // 10% more experience of every kind per rank
	TOUGH(Tree.SURVIVOR, 2),        // 2 armour per rank
	ADRENALINE(Tree.SURVIVOR, 1);   // ability: a burst of speed and resistance

	public static final Talent[] VALUES = values();
	public static final int PER_TREE = 5;
	/** Points that must already be spent in a tree before its ability can be bought. */
	public static final int CAPSTONE_POINTS = 6;

	public enum Tree {
		WARDEN, HUNTER, SURVIVOR
	}

	public final Tree tree;
	public final int maxRank;

	Talent(final Tree tree, final int maxRank) {
		this.tree = tree;
		this.maxRank = maxRank;
	}

	public int position() {
		return this.ordinal() % PER_TREE;
	}

	public boolean isAbility() {
		return this.position() == PER_TREE - 1;
	}

	/** Which ability slot (0 Warden, 1 Hunter, 2 Survivor) this talent unlocks; only meaningful for abilities. */
	public int abilitySlot() {
		return this.tree.ordinal();
	}

	public static Talent ability(final int slot) {
		return VALUES[slot * PER_TREE + PER_TREE - 1];
	}

	public String key() {
		return this.name().toLowerCase(java.util.Locale.ROOT);
	}
}

package fromthemist;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A talisman wards the room it hangs in, but only while that room is sealed: every door, trapdoor and
 * gate shut and no gap to the outside. Each talisman flood-fills the open space around it once a second;
 * if the fill stays contained, those blocks are the ward.
 */
public final class WardManager {
	private static final int MAX_CELLS = 2500;
	private static final int MAX_REACH = 26;
	private static final Direction[] DIRECTIONS = Direction.values();
	/** Talisman position to the sealed interior it protects. Overworld only. */
	private static final Map<Long, LongOpenHashSet> SEALED = new HashMap<>();

	private WardManager() {
	}

	public static void clear() {
		SEALED.clear();
	}

	public static void tick(final ServerLevel level, final MistState state) {
		long time = level.getGameTime();
		for (int i = state.talismans.size() - 1; i >= 0; i--) {
			if ((time + i) % 20L != 0L) {
				continue;
			}

			long key = state.talismans.get(i);
			BlockPos pos = BlockPos.of(key);
			if (!level.isLoaded(pos)) {
				SEALED.remove(key);
				continue;
			}

			BlockState block = level.getBlockState(pos);
			if (!block.is(ModContent.TALISMAN)) {
				state.talismans.remove(i);
				state.setDirty();
				SEALED.remove(key);
				continue;
			}

			LongOpenHashSet interior = block.getValue(TalismanBlock.CHARGE) > 0 ? flood(level, pos) : null;
			if (interior == null) {
				SEALED.remove(key);
			} else {
				SEALED.put(key, interior);
			}
		}
	}

	/** The open space reachable from the talisman, or null if it leaks to the outside. */
	private static LongOpenHashSet flood(final ServerLevel level, final BlockPos origin) {
		LongOpenHashSet seen = new LongOpenHashSet();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(origin.asLong());
		queue.add(origin);
		while (!queue.isEmpty()) {
			BlockPos pos = queue.poll();
			for (Direction direction : DIRECTIONS) {
				BlockPos next = pos.relative(direction);
				long key = next.asLong();
				if (seen.contains(key) || !passable(level, next)) {
					continue;
				}

				if (seen.size() >= MAX_CELLS || next.distManhattan(origin) > MAX_REACH || !level.isLoaded(next)) {
					return null;
				}

				seen.add(key);
				queue.add(next);
			}
		}

		return seen;
	}

	private static boolean passable(final ServerLevel level, final BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir()) {
			return true;
		}

		if (state.getBlock() instanceof DoorBlock) {
			return state.getValue(DoorBlock.OPEN);
		}

		if (state.getBlock() instanceof TrapDoorBlock) {
			return state.getValue(TrapDoorBlock.OPEN);
		}

		if (state.getBlock() instanceof FenceGateBlock) {
			return state.getValue(FenceGateBlock.OPEN);
		}

		return state.getCollisionShape(level, pos).isEmpty();
	}

	public static boolean isSealed(final BlockPos talisman) {
		return SEALED.containsKey(talisman.asLong());
	}

	/** True when the position is inside a sealed, warded room. */
	public static boolean isWarded(final Level level, final BlockPos pos) {
		if (level.dimension() != Level.OVERWORLD || SEALED.isEmpty()) {
			return false;
		}

		long feet = pos.asLong();
		long head = pos.above().asLong();
		for (LongOpenHashSet interior : SEALED.values()) {
			if (interior.contains(feet) || interior.contains(head)) {
				return true;
			}
		}

		return false;
	}

	/** True when the block is part of a warded room's shell (or inside it), so the night folk cannot break it. */
	public static boolean isProtected(final Level level, final BlockPos pos) {
		if (level.dimension() != Level.OVERWORLD || SEALED.isEmpty()) {
			return false;
		}

		long self = pos.asLong();
		for (LongOpenHashSet interior : SEALED.values()) {
			if (interior.contains(self)) {
				return true;
			}

			for (Direction direction : DIRECTIONS) {
				if (interior.contains(pos.relative(direction).asLong())) {
					return true;
				}
			}
		}

		return false;
	}
}

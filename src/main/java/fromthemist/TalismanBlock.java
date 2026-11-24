package fromthemist;

import java.util.Map;

import fromthemist.rpg.Rpg;
import fromthemist.rpg.Talent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** A carved stone hung on a wall. Its charge is the number of nights it will still hold. */
public class TalismanBlock extends Block {
	public static final int MAX_CHARGE = 7;
	public static final int ESSENCE_CHARGE = 3;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, MAX_CHARGE);
	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
		Direction.NORTH, Block.box(4.0, 3.0, 15.0, 12.0, 13.0, 16.0),
		Direction.SOUTH, Block.box(4.0, 3.0, 0.0, 12.0, 13.0, 1.0),
		Direction.WEST, Block.box(15.0, 3.0, 4.0, 16.0, 13.0, 12.0),
		Direction.EAST, Block.box(0.0, 3.0, 4.0, 1.0, 13.0, 12.0)
	);

	public TalismanBlock(final BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CHARGE, MAX_CHARGE));
	}

	@Override
	protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, CHARGE);
	}

	@Override
	protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockPos wall = pos.relative(facing.getOpposite());
		return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
	}

	@Override
	protected BlockState updateShape(
		final BlockState state,
		final LevelReader level,
		final ScheduledTickAccess ticks,
		final BlockPos pos,
		final Direction directionToNeighbour,
		final BlockPos neighbourPos,
		final BlockState neighbourState,
		final RandomSource random
	) {
		if (directionToNeighbour.getOpposite() == state.getValue(FACING) && !state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}

		return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
		BlockState state = this.defaultBlockState();
		for (Direction direction : context.getNearestLookingDirections()) {
			if (direction.getAxis().isHorizontal()) {
				state = state.setValue(FACING, direction.getOpposite());
				if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
					return state;
				}
			}
		}

		return null;
	}

	@Override
	protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
		if (level instanceof ServerLevel serverLevel && level.dimension() == Level.OVERWORLD) {
			MistState.get(serverLevel).addTalisman(pos);
		}
	}

	@Override
	protected InteractionResult useItemOn(
		final ItemStack itemStack,
		final BlockState state,
		final Level level,
		final BlockPos pos,
		final Player player,
		final InteractionHand hand,
		final BlockHitResult hitResult
	) {
		if (!itemStack.is(ModContent.MIST_ESSENCE)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		int charge = state.getValue(CHARGE);
		if (!level.isClientSide()) {
			if (charge >= MAX_CHARGE) {
				player.sendOverlayMessage(Component.translatable("message.fromthemist.talisman_full"));
			} else {
				int charged = Math.min(MAX_CHARGE, charge + ESSENCE_CHARGE + Rpg.rank(player, Talent.STEADY_HANDS));
				level.setBlock(pos, state.setValue(CHARGE, charged), Block.UPDATE_ALL);
				itemStack.consume(1, player);
				level.playSound(null, pos, ModSounds.TALISMAN_FEED, SoundSource.BLOCKS, 1.0F, 1.0F);
				player.sendOverlayMessage(Component.translatable("message.fromthemist.talisman_fed", charged));
			}
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			int charge = state.getValue(CHARGE);
			Component message;
			if (level.dimension() != Level.OVERWORLD) {
				message = Component.translatable("message.fromthemist.talisman_wrong_place");
			} else if (charge <= 0) {
				message = Component.translatable("message.fromthemist.talisman_dead");
			} else if (WardManager.isSealed(pos)) {
				message = Component.translatable("message.fromthemist.talisman_sealed", charge);
			} else {
				message = Component.translatable("message.fromthemist.talisman_open", charge);
			}

			player.sendOverlayMessage(message);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	protected BlockState rotate(final BlockState state, final Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(final BlockState state, final Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}

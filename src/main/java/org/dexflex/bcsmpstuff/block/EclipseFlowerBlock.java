package org.dexflex.bcsmpstuff.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class EclipseFlowerBlock extends TallPlantBlock {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;

    public EclipseFlowerBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(HALF, DoubleBlockHalf.LOWER).with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isFullCube(world, pos) && !floor.isOf(Blocks.AIR);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        DoubleBlockHalf half = state.get(HALF);
        BlockPos below = pos.down();
        BlockState belowState = world.getBlockState(below);
        if (half == DoubleBlockHalf.LOWER) {
            return canPlantOnTop(belowState, world, below);
        } else {
            return belowState.isOf(this) && belowState.get(HALF) == DoubleBlockHalf.LOWER;
        }
    }


    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        if (pos.getY() < world.getTopY() - 1 && world.getBlockState(pos.up()).canReplace(ctx)) {
            return getDefaultState().with(HALF, DoubleBlockHalf.LOWER).with(FACING, ctx.getPlayerFacing().getOpposite());
        }
        return null;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, net.minecraft.item.ItemStack itemStack) {
        world.setBlockState(pos.up(), getDefaultState().with(HALF, DoubleBlockHalf.UPPER).with(FACING, state.get(FACING)), 3);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.get(HALF);

        if (direction.getAxis() == Direction.Axis.Y) {
            if (half == DoubleBlockHalf.LOWER && direction == Direction.UP &&
                    (!neighborState.isOf(this) || neighborState.get(HALF) != DoubleBlockHalf.UPPER)) {
                return Blocks.AIR.getDefaultState();
            }

            if (half == DoubleBlockHalf.UPPER && direction == Direction.DOWN &&
                    (!neighborState.isOf(this) || neighborState.get(HALF) != DoubleBlockHalf.LOWER)) {
                return Blocks.AIR.getDefaultState();
            }
        }

        // Survival check (break if not valid position)
        if (!this.canPlaceAt(state, world, pos)) {
            return Blocks.AIR.getDefaultState();
        }

        return state;
    }
}

package org.dexflex.bcsmpstuff.block;

import net.minecraft.block.*;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.dexflex.bcsmpstuff.particle.ModParticles;

public class InkFlowerBlock extends TallPlantBlock {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;

    public InkFlowerBlock(Settings settings) {
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
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (state.get(HALF) == DoubleBlockHalf.UPPER && random.nextFloat() * 0.35f < 0.1f) {
            double x = pos.getX() + 0.5 + (random.nextFloat() - 0.4);
            double y = pos.getY() + 0.3;
            double z = pos.getZ() + 0.5 + (random.nextFloat() - 0.4);
            world.addParticle(ModParticles.INKFLOWER_SEEDS, x, y, z, 0, 0.02, 0);
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


        if (!this.canPlaceAt(state, world, pos)) {
            return Blocks.AIR.getDefaultState();
        }


        return state;
    }


    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(HALF) != DoubleBlockHalf.LOWER) return;


// Prevent eclipsing if on soul soil
        BlockState below = world.getBlockState(pos.down());
        if (below.isOf(Blocks.SOUL_SOIL)) return;


        if (!world.isDay() && world.getMoonPhase() == 4 && random.nextFloat() < 0.25f) {
            EclipseFlowerBlock.replaceWithConversion(world, pos, state.get(FACING),random);
        }
    }
}

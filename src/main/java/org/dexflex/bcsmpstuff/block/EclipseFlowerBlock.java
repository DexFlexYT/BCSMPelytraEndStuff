package org.dexflex.bcsmpstuff.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class EclipseFlowerBlock extends TallPlantBlock {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
    public static final IntProperty POWER = Properties.POWER;

    private static final int TICK_DELAY = 200;

    public EclipseFlowerBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState()
                .with(HALF, DoubleBlockHalf.LOWER)
                .with(FACING, Direction.NORTH)
                .with(POWER, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING, POWER);
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
        // Place upper block first with FORCE_STATE to prevent breaking
        world.setBlockState(pos.up(), getDefaultState().with(HALF, DoubleBlockHalf.UPPER).with(FACING, state.get(FACING)), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);

        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            int power = computeMoonPower(serverWorld);
            BlockState newLower = state.with(POWER, power);
            // Replace lower block using FORCE_STATE to prevent breaking
            world.setBlockState(pos, newLower, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            serverWorld.createAndScheduleBlockTick(pos, this, TICK_DELAY);
        }
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
    public boolean hasComparatorOutput(BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (state.get(HALF) == DoubleBlockHalf.LOWER) {
            return state.get(POWER);
        }
        return 0;
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(HALF) != DoubleBlockHalf.LOWER) return;

        int current = state.get(POWER);
        int computed = computeMoonPower(world);

        if (current != computed) {
            BlockState newState = state.with(POWER, computed);
            world.setBlockState(pos, newState, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            world.updateNeighborsAlways(pos, this);
            world.updateComparators(pos, this);
        }

        world.createAndScheduleBlockTick(pos, this, TICK_DELAY);
    }

    private static int computeMoonPower(ServerWorld world) {
        int moonPhase = world.getMoonPhase();
        int power = (int) Math.round(((double) moonPhase / 7.0) * 15.0);
        if (power < 0) power = 0;
        if (power > 15) {
        }
        return moonPhase;
    }

    /**
     * Replaces a given lower block with EclipseFlower safely, preventing breaking due to missing upper half.
     */
    public static void replaceWithConversion(ServerWorld world, BlockPos pos, Direction facing,Random random) {

        world.playSound(null, pos, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.BLOCKS, 1.0f, 0.625f+ random.nextFloat()*.25f );
        world.playSound(null, pos, SoundEvents.ITEM_BONE_MEAL_USE, SoundCategory.BLOCKS, 1.0f, 0.625f+ random.nextFloat()*.25f);

        BlockState newLower = ModBlocks.ECLIPSE_FLOWER.getDefaultState()
                .with(HALF, DoubleBlockHalf.LOWER)
                .with(FACING, facing);
        // Place upper block first with FORCE_STATE
        world.setBlockState(pos.up(), ModBlocks.ECLIPSE_FLOWER.getDefaultState()
                .with(HALF, DoubleBlockHalf.UPPER)
                .with(FACING, facing), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        // Then place lower block
        world.setBlockState(pos, newLower, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
    }
}

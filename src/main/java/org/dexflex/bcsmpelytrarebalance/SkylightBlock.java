package org.dexflex.bcsmpelytrarebalance;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class SkylightBlock extends Block {
    private static final VoxelShape SMALL_SHAPE = VoxelShapes.cuboid(
            3.0f / 16, 3.0f / 16, 3.0f / 16,
            13.0f / 16, 13.0f / 16, 13.0f / 16
    );

    public SkylightBlock(Settings settings) {
        super(settings
                .strength(0.3f)
                .luminance(state -> 15)
                .nonOpaque()
                .allowsSpawning((s, w, p, e) -> false)
                .solidBlock((s, w, p) -> true)
                .suffocates((s, w, p) -> false)
                .blockVision((s, w, p) -> false)
                .sounds(BlockSoundGroup.LANTERN)

        );
}

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SMALL_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SMALL_SHAPE;
    }
    @Override
    public boolean isSideInvisible(BlockState state, BlockState adjacent, Direction side) {
        return false;          // disable vanilla face culling
    }

    @Override
    public int getOpacity(BlockState state, BlockView world, BlockPos pos) {
        return 0;
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 1.0f;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        for (int i = 0; i < 1; i++) {
            double dx = pos.getX() + random.nextDouble() * 9 - 4;
            double dy = pos.getY() + random.nextDouble() * 9 - 4;
            double dz = pos.getZ() + random.nextDouble() * 9 - 4;
            world.addParticle(ParticleTypes.END_ROD, dx, dy, dz, 0, 0.01, 0);
        }

        double cx = pos.getX() + 0.5 + (random.nextDouble() * 0.2 - 0.1);
        double cy = pos.getY() + 0.5 + (random.nextDouble() * 0.2 - 0.1);
        double cz = pos.getZ() + 0.5 + (random.nextDouble() * 0.2 - 0.1);
        world.addParticle(ParticleTypes.END_ROD, cx, cy, cz, 0, 0.01, 0);
    }
}

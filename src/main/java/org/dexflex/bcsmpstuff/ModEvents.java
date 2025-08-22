package org.dexflex.bcsmpstuff;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.dexflex.bcsmpstuff.block.InkFlowerBlock;
import org.dexflex.bcsmpstuff.block.ModBlocks;

public class ModEvents {
    public static void registerEvents() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (world.isClient()) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            BlockState clickedState = world.getBlockState(pos);

            if (clickedState.isOf(Blocks.SUNFLOWER)) {
                if (player.getStackInHand(hand).isEmpty() || !player.getStackInHand(hand).isOf(Items.INK_SAC))
                    return ActionResult.PASS;

                if (clickedState.get(TallPlantBlock.HALF) == DoubleBlockHalf.UPPER) {
                    pos = pos.down();
                }
                world.playSound(null, pos, SoundEvents.ENTITY_SQUID_SQUIRT, SoundCategory.BLOCKS, 1.0f, 1.2f);
                world.playSound(null, pos, SoundEvents.ENTITY_DOLPHIN_SWIM, SoundCategory.BLOCKS, 1.0f, 1.5f);

                world.setBlockState(pos, Blocks.AIR.getDefaultState(),  Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                world.setBlockState(pos.up(), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);

                BlockState lower = ModBlocks.INK_FLOWER.getDefaultState()
                        .with(InkFlowerBlock.HALF, DoubleBlockHalf.LOWER)
                        .with(InkFlowerBlock.FACING, player.getHorizontalFacing().getOpposite());
                BlockState upper = ModBlocks.INK_FLOWER.getDefaultState()
                        .with(InkFlowerBlock.HALF, DoubleBlockHalf.UPPER)
                        .with(InkFlowerBlock.FACING, player.getHorizontalFacing().getOpposite());

                world.setBlockState(pos, lower, Block.NOTIFY_NEIGHBORS | Block.FORCE_STATE);
                world.setBlockState(pos.up(), upper, Block.NOTIFY_NEIGHBORS | Block.FORCE_STATE);

                if (!player.isCreative()) {
                    player.getStackInHand(hand).decrement(1);
                }
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }
}
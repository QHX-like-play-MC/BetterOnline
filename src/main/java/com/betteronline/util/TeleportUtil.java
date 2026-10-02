package com.betteronline.util;

import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

public class TeleportUtil {

    public static Text checkSafe(ServerWorld world, double x, double y, double z) {
        BlockPos feet = BlockPos.ofFloored(x, y, z);
        BlockPos head = feet.up();
        BlockPos below = feet.down();

        if (!world.getBlockState(below).isSolidBlock(world, below)) {
            return Text.literal("§c目标位置脚底没有实心方块！");
        }
        if (world.getBlockState(feet).isSolidBlock(world, feet)) {
            return Text.literal("§c目标位置被方块堵塞！坐标: " + feet.toShortString());
        }
        if (world.getBlockState(head).isSolidBlock(world, head)) {
            return Text.literal("§c目标位置头部空间不足！坐标: " + head.toShortString());
        }
        if (world.getBlockState(feet).isOf(Blocks.LAVA) || world.getBlockState(head).isOf(Blocks.LAVA)) {
            return Text.literal("§c目标位置在岩浆中！");
        }
        return null;
    }

    public static void teleport(ServerPlayerEntity player, ServerWorld world,
                                double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, Set.of(), yaw, pitch, true);
    }
}
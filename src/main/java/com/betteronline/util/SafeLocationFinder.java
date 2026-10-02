package com.betteronline.util;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

public class SafeLocationFinder {
    private static final int MAX_ATTEMPTS = 100;

    public record Result(double x, double y, double z, boolean success) {}

    public static Result find(ServerWorld world, double centerX, double centerZ,
                              int radius, boolean circle) {
        Random random = world.getRandom();
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            double dx, dz;
            if (circle) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double r = radius * Math.sqrt(random.nextDouble());
                dx = Math.cos(angle) * r;
                dz = Math.sin(angle) * r;
            } else {
                dx = (random.nextDouble() * 2 - 1) * radius;
                dz = (random.nextDouble() * 2 - 1) * radius;
            }
            int x = (int) (centerX + dx);
            int z = (int) (centerZ + dz);
            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
            for (int y = topY; y > world.getBottomY(); y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (isSafe(world, pos)) {
                    return new Result(x + 0.5, y + 0.1, z + 0.5, true);
                }
            }
        }
        return new Result(0, 0, 0, false);
    }

    private static boolean isSafe(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos.down()).isSolidBlock(world, pos.down())
                && world.getBlockState(pos).isAir()
                && world.getBlockState(pos.up()).isAir();
    }
}
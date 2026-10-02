package com.betteronline.event;

import com.betteronline.data.HomeData;
import com.betteronline.data.TpaManager;
import com.betteronline.gamerule.ModGameRules;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerEventListener {

    public static class DeathLocation {
        public final String world;
        public final double x, y, z;
        public final float yaw, pitch;

        public DeathLocation(String world, double x, double y, double z, float yaw, float pitch) {
            this.world = world; this.x = x; this.y = y; this.z = z;
            this.yaw = yaw; this.pitch = pitch;
        }
    }

    private static final Map<UUID, DeathLocation> LAST_DEATH = new HashMap<>();
    private static final Map<UUID, Vec3d> LOCKED_POS = new HashMap<>();

    public static void register() {
        // Home 数据生命周期
        ServerLifecycleEvents.SERVER_STARTING.register(HomeData::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(HomeData::onServerStopping);

        // ---- 死亡记录（精确记录死亡瞬间）----
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) {
                ServerWorld world = player.getEntityWorld();
                LAST_DEATH.put(player.getUuid(), new DeathLocation(
                    world.getRegistryKey().getValue().toString(),
                    player.getX(), player.getY(), player.getZ(),
                    player.getYaw(), player.getPitch()
                ));
            }
        });

        // ---- 下线清理 ----
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            TpaManager.clearPlayer(uuid);
            LOCKED_POS.remove(uuid);
        });

        // ---- PVP 拦截 ----
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!(entity instanceof PlayerEntity)) return ActionResult.PASS;
            if (!(world instanceof ServerWorld sw)) return ActionResult.PASS;

            boolean pvpEnabled = sw.getGameRules().getValue(ModGameRules.PVP);
            if (!pvpEnabled) {
                sp.sendMessage(Text.literal("§cPVP 已被禁止！"), true);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        // ---- 移动锁定 ----
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean moveEnabled = server.getOverworld().getGameRules().getValue(ModGameRules.MOVE);
            if (moveEnabled) {
                LOCKED_POS.clear();
                return;
            }
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID uuid = player.getUuid();
                Vec3d current = new Vec3d(player.getX(), player.getY(), player.getZ());
                Vec3d locked = LOCKED_POS.get(uuid);
                if (locked == null) {
                    LOCKED_POS.put(uuid, current);
                    continue;
                }
                if (current.squaredDistanceTo(locked) > 0.01) {
                    player.requestTeleport(locked.x, locked.y, locked.z);
                }
            }
        });
    }

    public static DeathLocation getLastDeath(UUID uuid) {
        return LAST_DEATH.get(uuid);
    }
}
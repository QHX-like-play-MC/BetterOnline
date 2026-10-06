package com.betteronline.event;

import com.betteronline.data.HomeData;
import com.betteronline.data.TpaManager;
import com.betteronline.gamerule.ModGameRules;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerAbilities;
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

        // ---- 玩家加入时同步飞行状态 ----
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            boolean flyEnabled = server.getOverworld().getGameRules()
                    .getValue(ModGameRules.FLY);
            PlayerAbilities abilities = player.getAbilities();

            if (flyEnabled && !abilities.allowFlying) {
                abilities.allowFlying = true;
                player.sendAbilitiesUpdate();
            } else if (!flyEnabled && abilities.allowFlying) {
                abilities.allowFlying = false;
                abilities.flying = false;
                player.sendAbilitiesUpdate();
            }
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

        // ---- 移动锁定 + fly 规则同步 ----
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // 移动锁定
            boolean moveEnabled = server.getOverworld().getGameRules()
                    .getValue(ModGameRules.MOVE);
            if (moveEnabled) {
                LOCKED_POS.clear();
            } else {
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
            }

            // fly 规则同步
            boolean flyEnabled = server.getOverworld().getGameRules()
                    .getValue(ModGameRules.FLY);
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                PlayerAbilities abilities = player.getAbilities();
                if (flyEnabled) {
                    if (!abilities.allowFlying) {
                        abilities.allowFlying = true;
                        player.sendAbilitiesUpdate();
                    }
                } else {
                    if (abilities.allowFlying) {
                        abilities.allowFlying = false;
                        if (abilities.flying) abilities.flying = false;
                        player.sendAbilitiesUpdate();
                    }
                }
            }
        });

        // 注意：nocooldown 逻辑由 PlayerEntityMixin 处理
    }

    public static DeathLocation getLastDeath(UUID uuid) {
        return LAST_DEATH.get(uuid);
    }
}
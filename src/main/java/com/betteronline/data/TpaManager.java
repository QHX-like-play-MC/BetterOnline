package com.betteronline.data;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TpaManager {
    public static class TpaRequest {
        public final ServerPlayerEntity sender;
        public final ServerPlayerEntity target;
        public final boolean here;      // true = /tpahere (target 传送到 sender)
        public final long timestamp;

        public TpaRequest(ServerPlayerEntity sender, ServerPlayerEntity target, boolean here) {
            this.sender = sender; this.target = target; this.here = here;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // target UUID -> 该玩家收到的所有待处理请求
    private static final Map<UUID, List<TpaRequest>> PENDING = new ConcurrentHashMap<>();
    // sender UUID -> 上次发送时间（冷却）
    private static final Map<UUID, Long> COOLDOWN = new ConcurrentHashMap<>();

    private static final long COOLDOWN_MS = 5_000;
    private static final long TIMEOUT_MS = 60_000;

    /** 检查冷却，返回剩余毫秒（0 表示可发送） */
    public static long getCooldownRemaining(UUID sender) {
        Long last = COOLDOWN.get(sender);
        if (last == null) return 0;
        long remain = COOLDOWN_MS - (System.currentTimeMillis() - last);
        return Math.max(0, remain);
    }

    public static void markSent(UUID sender) {
        COOLDOWN.put(sender, System.currentTimeMillis());
    }

    public static void addRequest(TpaRequest req) {
        PENDING.computeIfAbsent(req.target.getUuid(), k -> new ArrayList<>()).add(req);
    }

    /** 获取该玩家最新的请求（列表末尾） */
    public static TpaRequest getLatest(UUID target) {
        List<TpaRequest> list = PENDING.get(target);
        if (list == null || list.isEmpty()) return null;
        return list.get(list.size() - 1);
    }

    /** 移除指定请求（accept/refuse 时调用） */
    public static void removeRequest(TpaRequest req) {
        List<TpaRequest> list = PENDING.get(req.target.getUuid());
        if (list != null) list.remove(req);
    }

    /** 清理超时请求（由 ServerTickHandler 调用） */
    public static void cleanTimeout() {
        long now = System.currentTimeMillis();
        PENDING.values().forEach(list ->
            list.removeIf(r -> now - r.timestamp > TIMEOUT_MS)
        );
    }

    /** 玩家下线时清理所有相关请求 */
    public static void clearPlayer(UUID uuid) {
        PENDING.remove(uuid);
        PENDING.values().forEach(list ->
            list.removeIf(r -> r.sender.getUuid().equals(uuid))
        );
    }
}
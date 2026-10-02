package com.betteronline.data;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class HomeData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, PlayerHomes> DATA = new HashMap<>();
    private static MinecraftServer server;

    public static class PlayerHomes {
        public int maxHomes = 5;                    // 默认限额 5
        public Map<String, HomeLocation> homes = new LinkedHashMap<>();
    }

    public static class HomeLocation {
        public String world;    // 维度 ID
        public double x, y, z;
        public float yaw, pitch;

        public HomeLocation() {}
        public HomeLocation(String world, double x, double y, double z, float yaw, float pitch) {
            this.world = world; this.x = x; this.y = y; this.z = z;
            this.yaw = yaw; this.pitch = pitch;
        }
    }

    // ---- 生命周期 ----
    public static void onServerStarting(MinecraftServer srv) {
        server = srv;
        DATA.clear();
        Path savePath = srv.getSavePath(WorldSavePath.ROOT).resolve("betteronline_homes.json");
        File file = savePath.toFile();
        if (file.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                    UUID uuid = UUID.fromString(entry.getKey());
                    PlayerHomes ph = GSON.fromJson(entry.getValue(), PlayerHomes.class);
                    DATA.put(uuid, ph);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void onServerStopping(MinecraftServer srv) {
        save();
    }

    public static void save() {
        if (server == null) return;
        Path savePath = server.getSavePath(WorldSavePath.ROOT).resolve("betteronline_homes.json");
        JsonObject root = new JsonObject();
        for (Map.Entry<UUID, PlayerHomes> entry : DATA.entrySet()) {
            root.add(entry.getKey().toString(), GSON.toJsonTree(entry.getValue()));
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(savePath.toFile()), StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---- 访问方法 ----
    public static PlayerHomes get(UUID uuid) {
        return DATA.computeIfAbsent(uuid, k -> new PlayerHomes());
    }

    public static boolean addHome(UUID uuid, String name, HomeLocation loc) {
        PlayerHomes ph = get(uuid);
        if (ph.homes.size() >= ph.maxHomes) return false;
        ph.homes.put(name, loc);
        save();
        return true;
    }

    public static boolean removeHome(UUID uuid, String name) {
        PlayerHomes ph = get(uuid);
        boolean removed = ph.homes.remove(name) != null;
        if (removed) save();
        return removed;
    }

    public static HomeLocation getHome(UUID uuid, String name) {
        return get(uuid).homes.get(name);
    }

    public static void increaseLimit(UUID uuid, int amount) {
        get(uuid).maxHomes += amount;
        save();
    }
}
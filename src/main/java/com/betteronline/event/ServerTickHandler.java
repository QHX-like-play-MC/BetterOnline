package com.betteronline.event;

import com.betteronline.command.TeleportCommands;
import com.betteronline.data.TpaManager;
import com.betteronline.util.TeleportUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.util.Iterator;

public class ServerTickHandler {
    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ServerTickHandler::onTick);
    }

    private static void onTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % 20 == 0) {
            TpaManager.cleanTimeout();
        }
        processCountdowns();
    }

    private static void processCountdowns() {
        Iterator<TeleportCommands.CountdownTask> it = TeleportCommands.COUNTDOWNS.iterator();
        while (it.hasNext()) {
            TeleportCommands.CountdownTask task = it.next();
            int t = task.ticksElapsed;

            // 每秒显示一次倒计时（t = 0, 20, 40）
            if (t == 0 || t == 20 || t == 40) {
                int secondsLeft = 3 - (t / 20);
                task.teleporter.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 15, 5));
                task.teleporter.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§e即将传送")));
                task.teleporter.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("§c" + secondsLeft + " 秒")));
            }

            // 第 60 tick（第 4 秒）执行传送
            if (t == 60) {
                task.teleporter.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 15, 5));
                task.teleporter.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§a传送成功！")));
                TeleportUtil.teleport(task.teleporter, task.destWorld,
                    task.dx, task.dy, task.dz, task.dyaw, task.dpitch);
            }

            task.ticksElapsed++;
            if (task.ticksElapsed > 80) { // 4 秒后清理
                it.remove();
            }
        }
    }
}
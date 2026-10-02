package com.betteronline;

import com.betteronline.command.CommandRegistry;
import com.betteronline.data.HomeData;
import com.betteronline.event.PlayerEventListener;
import com.betteronline.event.ServerTickHandler;
import com.betteronline.gamerule.ModGameRules;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class BetterOnline implements ModInitializer {
    public static final String MOD_ID = "betteronline";

    @Override
    public void onInitialize() {
        // 1. 注册自定义 GameRule
        ModGameRules.register();

        // 2. 注册所有命令
        CommandRegistry.register();

        // 3. 注册事件监听
        PlayerEventListener.register();
        ServerTickHandler.register();

        // 4. Home 数据生命周期（加载 / 保存）
        ServerLifecycleEvents.SERVER_STARTING.register(HomeData::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(HomeData::onServerStopping);
    }
}
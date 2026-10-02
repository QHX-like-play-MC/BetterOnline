package com.betteronline.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class CommandRegistry {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TeleportCommands.register(dispatcher);
            HomeCommands.register(dispatcher);
            StatusCommands.register(dispatcher);
            ChatCommands.register(dispatcher);
            AdminCommands.register(dispatcher);
        });
    }
}
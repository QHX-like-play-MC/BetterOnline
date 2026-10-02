package com.betteronline.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ChatCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("msg")
            .then(CommandManager.argument("target", EntityArgumentType.player())
                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                    .executes(ChatCommands::executeMsg))));
    }

    private static int executeMsg(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity sender = ctx.getSource().getPlayer();
        if (sender == null) { ctx.getSource().sendError(Text.literal("必须由玩家执行")); return 0; }
        try {
            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
            String message = StringArgumentType.getString(ctx, "message");

            sender.sendMessage(Text.literal("§7[我 -> " + target.getName().getString() + "] §f" + message), false);
            target.sendMessage(Text.literal("§7[" + sender.getName().getString() + " -> 我] §f" + message), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("§c玩家未找到"));
            return 0;
        }
    }
}
package com.betteronline.command;

import com.betteronline.gamerule.ModGameRules;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

public class StatusCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("heal")
            .executes(StatusCommands::executeHealSelf)
            .then(CommandManager.argument("target", EntityArgumentType.players())
                .requires(source -> Permissions.check(source, "betteronline.heal.others", 2))
                .executes(StatusCommands::executeHealOther)));

        dispatcher.register(CommandManager.literal("feed")
            .executes(StatusCommands::executeFeedSelf)
            .then(CommandManager.argument("target", EntityArgumentType.players())
                .requires(source -> Permissions.check(source, "betteronline.feed.others", 2))
                .executes(StatusCommands::executeFeedOther)));
    }

    private static int executeHealSelf(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) { ctx.getSource().sendError(Text.literal("必须由玩家执行")); return 0; }
        if (!player.getEntityWorld().getGameRules().getValue(ModGameRules.CAN_HEAL)) {
            ctx.getSource().sendError(Text.literal("§cGamerule canheal 已经被禁止使用！"));
            return 0;
        }
        doHeal(player);
        ctx.getSource().sendFeedback(() -> Text.literal("§a已恢复满血量并清除负面效果"), false);
        return 1;
    }

    /** OP 治疗他人：绕过 gamerule 限制 */
    private static int executeHealOther(CommandContext<ServerCommandSource> ctx) {
        try {
            Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "target");
            for (ServerPlayerEntity p : targets) {
                doHeal(p);
                p.sendMessage(Text.literal("§a你已被治疗"), false);
            }
            ctx.getSource().sendFeedback(() -> Text.literal("§a已治疗 " + targets.size() + " 名玩家"), false);
            return targets.size();
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("§c玩家未找到"));
            return 0;
        }
    }

    private static void doHeal(ServerPlayerEntity player) {
        player.setHealth(player.getMaxHealth());
        player.getStatusEffects().stream()
            .map(StatusEffectInstance::getEffectType)
            .toList()
            .forEach(player::removeStatusEffect);
        player.getHungerManager().setFoodLevel(20);
    }

    private static int executeFeedSelf(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) { ctx.getSource().sendError(Text.literal("必须由玩家执行")); return 0; }
        if (!player.getEntityWorld().getGameRules().getValue(ModGameRules.CAN_FEED)) {
            ctx.getSource().sendError(Text.literal("§cGamerule canfeed 已经被禁止使用！"));
            return 0;
        }
        player.getHungerManager().setFoodLevel(20);
        player.getHungerManager().setSaturationLevel(5.0f);
        ctx.getSource().sendFeedback(() -> Text.literal("§a已恢复满饱食度"), false);
        return 1;
    }

    /** OP 喂食他人：绕过 gamerule 限制 */
    private static int executeFeedOther(CommandContext<ServerCommandSource> ctx) {
        try {
            Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "target");
            for (ServerPlayerEntity p : targets) {
                p.getHungerManager().setFoodLevel(20);
                p.getHungerManager().setSaturationLevel(5.0f);
                p.sendMessage(Text.literal("§a你已被喂饱"), false);
            }
            ctx.getSource().sendFeedback(() -> Text.literal("§a已喂饱 " + targets.size() + " 名玩家"), false);
            return targets.size();
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("§c玩家未找到"));
            return 0;
        }
    }
}
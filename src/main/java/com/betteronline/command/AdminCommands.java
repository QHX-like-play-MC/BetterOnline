package com.betteronline.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class AdminCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("op")
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .executes(AdminCommands::executeOp)));

        dispatcher.register(CommandManager.literal("deop")
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .executes(AdminCommands::executeDeop)));

        dispatcher.register(CommandManager.literal("boh")
                .executes(ctx -> executeBoh(ctx, 1))
                .then(CommandManager.argument("page", IntegerArgumentType.integer(1, 2))
                        .executes(ctx -> executeBoh(ctx, IntegerArgumentType.getInteger(ctx, "page")))));
    }

    private static boolean isHost(ServerCommandSource source) {
        if (source.getServer().isSingleplayer()) return true;
        if (source.getEntity() == null) return true;
        return Permissions.check(source, "betteronline.host", 3);
    }

    private static int executeOp(CommandContext<ServerCommandSource> ctx) {
        if (!isHost(ctx.getSource())) {
            ctx.getSource().sendError(Text.literal("§c仅限主机使用！"));
            return 0;
        }
        try {
            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
            ctx.getSource().getServer().getPlayerManager()
                    .addToOperators(new PlayerConfigEntry(target.getGameProfile()));
            ctx.getSource().getServer().getCommandManager().sendCommandTree(target);
            ctx.getSource().sendFeedback(
                    () -> Text.literal("§a已将 " + target.getName().getString() + " 设为 OP"), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("§c玩家未找到"));
            return 0;
        }
    }

    private static int executeDeop(CommandContext<ServerCommandSource> ctx) {
        if (!isHost(ctx.getSource())) {
            ctx.getSource().sendError(Text.literal("§c仅限主机使用！"));
            return 0;
        }
        try {
            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
            if (ctx.getSource().getServer().isSingleplayer()
                    && Permissions.check(target, "betteronline.host", 4)) {
                ctx.getSource().sendError(Text.literal("§c无法取消主机自身的 OP 权限！"));
                return 0;
            }
            ctx.getSource().getServer().getPlayerManager()
                    .removeFromOperators(new PlayerConfigEntry(target.getGameProfile()));
            ctx.getSource().getServer().getCommandManager().sendCommandTree(target);
            ctx.getSource().sendFeedback(
                    () -> Text.literal("§c已取消 " + target.getName().getString() + " 的 OP"), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("§c玩家未找到"));
            return 0;
        }
    }

    // ============ /boh 详细帮助菜单 ============
    private static int executeBoh(CommandContext<ServerCommandSource> ctx, int page) {
        var src = ctx.getSource();

        src.sendFeedback(() -> Text.literal("§6§m                                             "), false);

        if (page == 1) {
            src.sendFeedback(() -> Text.literal("§6§l          BetterOnline 帮助菜单 §7(1/2)"), false);
            src.sendFeedback(() -> Text.literal("§6§m                                             "), false);

            src.sendFeedback(() -> Text.literal("§e§l【传送系统】"), false);
            sendHelp(src, "§e/tpa <玩家>", "/tpa ",
                    "请求传送到指定玩家身边", "§75 秒冷却，防刷屏");
            sendHelp(src, "§e/tpahere <玩家>", "/tpahere ",
                    "请求指定玩家传送到你身边", "§75 秒冷却，防刷屏");
            sendHelp(src, "§e/tpaaccept", "/tpaaccept",
                    "同意最近的传送请求", "§7敲钟 + 黑暗 + 3 秒倒计时");
            sendHelp(src, "§e/tparefuse", "/tparefuse",
                    "拒绝最近的传送请求", "§7通知双方，解除锁定");
            sendHelp(src, "§e/back", "/back",
                    "传送回上一次死亡的地点", "§7自动记录死亡坐标与维度");
            sendHelp(src, "§e/spawn", "/spawn",
                    "传送回世界的主出生点", "§7快速往返出生点与基地");
            sendHelp(src, "§e/rtp <x> <z> [true|false]", "/rtp 0 0 ",
                    "随机传送到安全位置", "§7true=圆形, false=方形, 默认圆形, 5000 格范围");
            sendHelp(src, "§e/tpall", "/tpall",
                    "召集所有在线玩家到你身边", "§c仅限 OP（等级 ≥ 2）");

            src.sendFeedback(() -> Text.literal("§e§l【家系统】§7（初始 5 个家，中文命名需加双引号）"), false);
            sendHelp(src, "§e/home list", "/home list",
                    "列出所有你设置的家", "§7点击家名可直接传送");
            sendHelp(src, "§e/home add \"名字\"", "/home add \"\"",
                    "在当前位置设置一个新家", "§7例：§f/home add \"我的家\"");
            sendHelp(src, "§e/home remove \"名字\"", "/home remove \"\"",
                    "删除指定的家", "§7例：§f/home remove \"我的家\"");
            sendHelp(src, "§e/home tp \"名字\"", "/home tp \"\"",
                    "传送到指定的家", "§7带安全检测，被堵塞或岩浆中拒绝传送");
            sendHelp(src, "§e/home buy <数量>", "/home buy ",
                    "购买额外的家的额度", "§a每 1 个额度消耗 5 个钻石");

        } else if (page == 2) {
            src.sendFeedback(() -> Text.literal("§6§l          BetterOnline 帮助菜单 §7(2/2)"), false);
            src.sendFeedback(() -> Text.literal("§6§m                                             "), false);

            src.sendFeedback(() -> Text.literal("§e§l【状态管理】"), false);
            sendHelp(src, "§e/heal", "/heal",
                    "恢复自身满血量并清除负面效果", "§7受 gamerule §fcanheal §7限制");
            sendHelp(src, "§e/heal <玩家>", "/heal ",
                    "恢复指定玩家的满血量", "§c仅限 OP（等级 ≥ 2），无视 canheal 限制");
            sendHelp(src, "§e/feed", "/feed",
                    "恢复自身满饱食度", "§7受 gamerule §fcanfeed §7限制");
            sendHelp(src, "§e/feed <玩家>", "/feed ",
                    "恢复指定玩家的饱食度", "§c仅限 OP（等级 ≥ 2），无视 canfeed 限制");

            src.sendFeedback(() -> Text.literal("§e§l【聊天系统】"), false);
            sendHelp(src, "§e/msg <玩家> <消息>", "/msg ",
                    "向指定玩家发送私密消息", "§7格式：§f[我 -> 玩家] 消息");

            src.sendFeedback(() -> Text.literal("§e§l【权限与管理】"), false);
            sendHelp(src, "§e/op <玩家>", "/op ",
                    "给予指定玩家 OP 权限", "§c仅限主机使用，授予后即时刷新命令树");
            sendHelp(src, "§e/deop <玩家>", "/deop ",
                    "取消指定玩家的 OP 权限", "§c仅限主机使用，无法取消主机自身 OP");
            sendHelp(src, "§e/boh [页码]", "/boh ",
                    "显示本帮助菜单", "§7例：§f/boh 2 §7跳到第二页");

            src.sendFeedback(() -> Text.literal("§e§l【游戏规则】§7（点击直接填入 /gamerule 命令）"), false);
            sendHelp(src, "§fbetteronline:pvp", "/gamerule betteronline:pvp ",
                    "§7允许或禁止玩家之间的 PVP 伤害", "§7默认：§atrue");
            sendHelp(src, "§fbetteronline:canheal", "/gamerule betteronline:canheal ",
                    "§7全局启用或禁用 /heal 自身", "§7默认：§atrue");
            sendHelp(src, "§fbetteronline:canfeed", "/gamerule betteronline:canfeed ",
                    "§7全局启用或禁用 /feed 自身", "§7默认：§atrue");
            sendHelp(src, "§fbetteronline:nocooldown", "/gamerule betteronline:nocooldown ",
                    "§7禁用攻击冷却，造成满额伤害", "§7默认：§cfalse");
            sendHelp(src, "§fbetteronline:move", "/gamerule betteronline:move ",
                    "§7允许玩家移动，false 则强制拉回", "§7默认：§atrue");
            sendHelp(src, "§fbetteronline:fly", "/gamerule betteronline:fly ",
                    "§7预留规则（暂未实现）", "§7默认：§cfalse");
        }

        src.sendFeedback(() -> Text.literal("§6§m                                             "), false);

        // ---- 翻页按钮 ----
        MutableText prev = Text.literal("§7[◀ 上一页] ");
        MutableText next = Text.literal("§7[下一页 ▶] ");
        MutableText pageInfo = Text.literal("§7第 " + page + " / 2 页  ");

        if (page == 1) {
            prev.setStyle(prev.getStyle().withColor(net.minecraft.util.Formatting.DARK_GRAY));
            next.setStyle(next.getStyle()
                    .withClickEvent(new ClickEvent.RunCommand("/boh 2"))
                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("§e点击切换到第 2 页"))));
        } else {
            prev.setStyle(prev.getStyle()
                    .withClickEvent(new ClickEvent.RunCommand("/boh 1"))
                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("§e点击切换到第 1 页"))));
            next.setStyle(next.getStyle().withColor(net.minecraft.util.Formatting.DARK_GRAY));
        }

        MutableText navLine = Text.literal("");
        navLine.append(prev);
        navLine.append(pageInfo);
        navLine.append(next);
        src.sendFeedback(() -> navLine, false);
        return 1;
    }

    /**
     * 输出一行帮助。
     * @param display 显示的文本（可带颜色代码）
     * @param suggest 点击后填入聊天栏的**完整命令**（必须以 / 开头）
     * @param desc    灰色说明
     * @param hover   悬停提示
     */
    private static void sendHelp(ServerCommandSource src, String display, String suggest, String desc, String hover) {
        MutableText line = Text.literal("  §a▶ ");
        MutableText cmd = Text.literal(display);
        cmd.setStyle(cmd.getStyle()
                .withClickEvent(new ClickEvent.SuggestCommand(suggest))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("§e" + suggest + "\n§7" + hover))));
        line.append(cmd);
        line.append(Text.literal(" §7- " + desc));
        src.sendFeedback(() -> line, false);
    }
}
package com.betteronline.command;

import com.betteronline.data.HomeData;
import com.betteronline.util.TeleportUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class HomeCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("home")
                .then(CommandManager.literal("list")
                        .executes(HomeCommands::executeList))
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .executes(HomeCommands::executeAdd)))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .executes(HomeCommands::executeRemove)))
                .then(CommandManager.literal("tp")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .executes(HomeCommands::executeTp)))
                .then(CommandManager.literal("buy")
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                .executes(HomeCommands::executeBuy))));
    }

    private static int executeList(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) { ctx.getSource().sendError(Text.literal("必须由玩家执行")); return 0; }
        HomeData.PlayerHomes ph = HomeData.get(player.getUuid());
        ctx.getSource().sendFeedback(() -> Text.literal(
                "§6===== 你的家 (" + ph.homes.size() + "/" + ph.maxHomes + ") ====="), false);
        if (ph.homes.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("§7暂无家，使用 /home add \"名字\" 创建"), false);
        } else {
            for (String name : ph.homes.keySet()) {
                MutableText line = Text.literal("§e- " + name + " ");
                line.setStyle(line.getStyle()
                        .withClickEvent(new ClickEvent.RunCommand("/home tp \"" + name + "\""))
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("点击传送到 " + name))));
                ctx.getSource().sendFeedback(() -> line, false);
            }
        }
        return 1;
    }

    private static int executeAdd(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) { ctx.getSource().sendError(Text.literal("必须由玩家执行")); return 0; }
        String name = StringArgumentType.getString(ctx, "name");
        HomeData.PlayerHomes ph = HomeData.get(player.getUuid());
        if (ph.homes.size() >= ph.maxHomes) {
            ctx.getSource().sendError(Text.literal("§c已达上限 (" + ph.maxHomes + ")，使用 /home buy <数量> 购买"));
            return 0;
        }
        if (ph.homes.containsKey(name)) {
            ctx.getSource().sendError(Text.literal("§c已存在同名的家"));
            return 0;
        }
        HomeData.HomeLocation loc = new HomeData.HomeLocation(
                player.getEntityWorld().getRegistryKey().getValue().toString(),
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch());
        HomeData.addHome(player.getUuid(), name, loc);
        ctx.getSource().sendFeedback(() -> Text.literal("§a已创建家: §e" + name), false);
        return 1;
    }

    private static int executeRemove(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) { ctx.getSource().sendError(Text.literal("必须由玩家执行")); return 0; }
        String name = StringArgumentType.getString(ctx, "name");
        if (HomeData.removeHome(player.getUuid(), name)) {
            ctx.getSource().sendFeedback(() -> Text.literal("§a已删除家: §e" + name), false);
            return 1;
        } else {
            ctx.getSource().sendError(Text.literal("§c未找到名为 " + name + " 的家"));
            return 0;
        }
    }

    private static int executeTp(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) { ctx.getSource().sendError(Text.literal("必须由玩家执行")); return 0; }
        String name = StringArgumentType.getString(ctx, "name");
        HomeData.HomeLocation loc = HomeData.getHome(player.getUuid(), name);
        if (loc == null) { ctx.getSource().sendError(Text.literal("§c未找到该家")); return 0; }

        ServerWorld world = player.getEntityWorld().getServer().getWorld(
                RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(loc.world)));
        if (world == null) { ctx.getSource().sendError(Text.literal("§c目标维度不存在")); return 0; }

        Text error = TeleportUtil.checkSafe(world, loc.x, loc.y, loc.z);
        if (error != null) {
            ctx.getSource().sendError(error);
            ctx.getSource().sendError(Text.literal("§7坐标: " + String.format("%.1f, %.1f, %.1f", loc.x, loc.y, loc.z)));
            return 0;
        }
        TeleportUtil.teleport(player, world, loc.x, loc.y, loc.z, loc.yaw, loc.pitch);
        ctx.getSource().sendFeedback(() -> Text.literal("§a已传送到家: §e" + name), false);
        return 1;
    }

    private static int executeBuy(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) { ctx.getSource().sendError(Text.literal("必须由玩家执行")); return 0; }
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        int cost = amount * 5;
        int have = countDiamonds(player);
        if (have < cost) {
            ctx.getSource().sendError(Text.literal("§c需要 " + cost + " 个钻石，你只有 " + have + " 个"));
            return 0;
        }
        removeDiamonds(player, cost);
        HomeData.increaseLimit(player.getUuid(), amount);
        ctx.getSource().sendFeedback(() -> Text.literal("§a成功购买 " + amount + " 个家的额度！当前上限: "
                + HomeData.get(player.getUuid()).maxHomes), false);
        return 1;
    }

    private static int countDiamonds(ServerPlayerEntity player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.DIAMOND)) count += stack.getCount();
        }
        return count;
    }

    private static void removeDiamonds(ServerPlayerEntity player, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.DIAMOND)) {
                int take = Math.min(stack.getCount(), remaining);
                stack.decrement(take);
                remaining -= take;
                if (stack.isEmpty()) player.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }
        player.currentScreenHandler.syncState();
    }
}
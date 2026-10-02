package com.betteronline.command;

import com.betteronline.data.TpaManager;
import com.betteronline.event.PlayerEventListener;
import com.betteronline.util.SafeLocationFinder;
import com.betteronline.util.TeleportUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class TeleportCommands {

    // ============ TPA 倒计时任务 ============
    public static class CountdownTask {
        public final ServerPlayerEntity teleporter;   // 要传送的人（看到倒计时）
        public final ServerWorld destWorld;
        public final double dx, dy, dz;
        public final float dyaw, dpitch;
        public int ticksElapsed = 0;

        public CountdownTask(ServerPlayerEntity teleporter, ServerWorld destWorld,
                             double dx, double dy, double dz, float dyaw, float dpitch) {
            this.teleporter = teleporter;
            this.destWorld = destWorld;
            this.dx = dx; this.dy = dy; this.dz = dz;
            this.dyaw = dyaw; this.dpitch = dpitch;
        }
    }

    /** 由 ServerTickHandler 每 tick 消费 */
    public static final List<CountdownTask> COUNTDOWNS = new ArrayList<>();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tpa")
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .executes(ctx -> executeTpa(ctx, false))));

        dispatcher.register(CommandManager.literal("tpahere")
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .executes(ctx -> executeTpa(ctx, true))));

        dispatcher.register(CommandManager.literal("tpaaccept")
                .executes(TeleportCommands::executeAccept));

        dispatcher.register(CommandManager.literal("tparefuse")
                .executes(TeleportCommands::executeRefuse));

        dispatcher.register(CommandManager.literal("back")
                .executes(TeleportCommands::executeBack));

        dispatcher.register(CommandManager.literal("spawn")
                .executes(TeleportCommands::executeSpawn));

        dispatcher.register(CommandManager.literal("rtp")
                .then(CommandManager.argument("x", IntegerArgumentType.integer())
                        .then(CommandManager.argument("z", IntegerArgumentType.integer())
                                .executes(ctx -> executeRtp(ctx, true))
                                .then(CommandManager.argument("circle", BoolArgumentType.bool())
                                        .executes(ctx -> executeRtp(ctx, BoolArgumentType.getBool(ctx, "circle")))))));

        dispatcher.register(CommandManager.literal("tpall")
                .requires(source -> Permissions.check(source, "betteronline.tpall", 2))
                .executes(TeleportCommands::executeTpAll));
    }

    private static int executeTpa(CommandContext<ServerCommandSource> ctx, boolean here) {
        ServerPlayerEntity sender = ctx.getSource().getPlayer();
        if (sender == null) { ctx.getSource().sendError(Text.literal("必须由玩家执行")); return 0; }
        try {
            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
            if (sender == target) {
                ctx.getSource().sendError(Text.literal("§c不能向自己发送传送请求！"));
                return 0;
            }
            long remain = TpaManager.getCooldownRemaining(sender.getUuid());
            if (remain > 0) {
                ctx.getSource().sendError(Text.literal("§c请等待 " + (remain / 1000 + 1) + " 秒后再发送！"));
                return 0;
            }
            TpaManager.markSent(sender.getUuid());
            TpaManager.addRequest(new TpaManager.TpaRequest(sender, target, here));

            String type = here ? "tpahere" : "tpa";
            target.sendMessage(Text.literal("§e" + sender.getName().getString()
                    + " §f向你发送了 §a/" + type + " §f请求，使用 §a/tpaaccept §f或 §c/tparefuse"), false);
            sender.sendMessage(Text.literal("§a已向 " + target.getName().getString() + " 发送请求"), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("§c玩家未找到"));
            return 0;
        }
    }

    /**
     * 接受传送请求：
     * - 判断“要传送的人”(teleporter) 和“目的地玩家”(destination)
     * - 给接受者显示“请求接受成功！”
     * - 给 teleporter 加敲钟 + 黑暗效果，并注册倒计时任务
     */
    private static int executeAccept(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) { ctx.getSource().sendError(Text.literal("必须由玩家执行")); return 0; }

        TpaManager.TpaRequest req = TpaManager.getLatest(player.getUuid());
        if (req == null) {
            ctx.getSource().sendError(Text.literal("§c没有待处理的传送请求"));
            return 0;
        }
        TpaManager.removeRequest(req);

        ServerPlayerEntity sender = req.sender;
        ServerPlayerEntity teleporter;
        ServerPlayerEntity destination;
        if (req.here) {
            // /tpahere：sender 请求 player 传送到 sender 身边 → player 是 teleporter
            teleporter = player;
            destination = sender;
        } else {
            // /tpa：sender 请求传送到 player 身边 → sender 是 teleporter
            teleporter = sender;
            destination = player;
        }

        // 接受者：显示“请求接受成功！”
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 10));
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("§a请求接受成功！")));

        // teleporter：敲钟 + 黑暗 + 倒计时
        ServerWorld world = teleporter.getEntityWorld();
        world.playSound(null, teleporter.getBlockPos(),
                SoundEvents.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
        teleporter.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.DARKNESS, 60, 0, false, false));

        COUNTDOWNS.add(new CountdownTask(
                teleporter,
                destination.getEntityWorld(),
                destination.getX(), destination.getY(), destination.getZ(),
                destination.getYaw(), destination.getPitch()
        ));
        return 1;
    }

    private static int executeRefuse(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) { ctx.getSource().sendError(Text.literal("必须由玩家执行")); return 0; }
        TpaManager.TpaRequest req = TpaManager.getLatest(player.getUuid());
        if (req == null) {
            ctx.getSource().sendError(Text.literal("§c没有待处理的传送请求"));
            return 0;
        }
        TpaManager.removeRequest(req);
        req.sender.sendMessage(Text.literal("§c你的传送请求被 " + player.getName().getString() + " 拒绝了"), false);
        player.sendMessage(Text.literal("§a你拒绝了传送请求"), true);
        return 1;
    }

    private static int executeBack(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) { ctx.getSource().sendError(Text.literal("必须由玩家执行")); return 0; }
        PlayerEventListener.DeathLocation loc = PlayerEventListener.getLastDeath(player.getUuid());
        if (loc == null) {
            ctx.getSource().sendError(Text.literal("§c没有死亡记录"));
            return 0;
        }
        ServerWorld world = player.getEntityWorld().getServer().getWorld(
                RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(loc.world)));
        if (world == null) { ctx.getSource().sendError(Text.literal("§c目标维度不存在")); return 0; }
        TeleportUtil.teleport(player, world, loc.x, loc.y, loc.z, loc.yaw, loc.pitch);
        ctx.getSource().sendFeedback(() -> Text.literal("§a已传送回死亡地点"), false);
        return 1;
    }

    private static int executeSpawn(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) { ctx.getSource().sendError(Text.literal("必须由玩家执行")); return 0; }
        ServerWorld overworld = player.getEntityWorld().getServer().getOverworld();
        net.minecraft.util.math.BlockPos spawn = overworld.getSpawnPoint().getPos();
        TeleportUtil.teleport(player, overworld,
                spawn.getX() + 0.5, spawn.getY() + 0.1, spawn.getZ() + 0.5, 0, 0);
        ctx.getSource().sendFeedback(() -> Text.literal("§a已传送回出生点"), false);
        return 1;
    }

    private static int executeRtp(CommandContext<ServerCommandSource> ctx, boolean circle) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) { ctx.getSource().sendError(Text.literal("必须由玩家执行")); return 0; }
        int cx = IntegerArgumentType.getInteger(ctx, "x");
        int cz = IntegerArgumentType.getInteger(ctx, "z");
        ServerWorld world = player.getEntityWorld();

        SafeLocationFinder.Result result = SafeLocationFinder.find(world, cx, cz, 5000, circle);
        if (!result.success()) {
            ctx.getSource().sendError(Text.literal("§c未找到安全位置，请重试"));
            return 0;
        }
        TeleportUtil.teleport(player, world, result.x(), result.y(), result.z(), 0, 0);
        ctx.getSource().sendFeedback(() -> Text.literal("§a已随机传送到 §e" +
                String.format("%.0f, %.0f, %.0f", result.x(), result.y(), result.z())), false);
        return 1;
    }

    private static int executeTpAll(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) { ctx.getSource().sendError(Text.literal("必须由玩家执行")); return 0; }
        ServerWorld world = player.getEntityWorld();
        int count = 0;
        for (ServerPlayerEntity target : ctx.getSource().getServer().getPlayerManager().getPlayerList()) {
            if (target == player) continue;
            TeleportUtil.teleport(target, world,
                    player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
            count++;
        }
        int finalCount = count;
        ctx.getSource().sendFeedback(() -> Text.literal("§a已召集 " + finalCount + " 名玩家"), false);
        return count;
    }
}
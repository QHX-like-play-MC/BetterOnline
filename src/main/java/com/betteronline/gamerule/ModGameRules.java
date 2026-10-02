package com.betteronline.gamerule;

import com.betteronline.BetterOnline;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.world.rule.GameRule;

public class ModGameRules {
    public static final GameRule<Boolean> PVP =
            GameRuleBuilder.forBoolean(true)
                    .buildAndRegister(Identifier.of(BetterOnline.MOD_ID, "pvp"));

    public static final GameRule<Boolean> CAN_HEAL =
            GameRuleBuilder.forBoolean(true)
                    .buildAndRegister(Identifier.of(BetterOnline.MOD_ID, "canheal"));

    public static final GameRule<Boolean> CAN_FEED =
            GameRuleBuilder.forBoolean(true)
                    .buildAndRegister(Identifier.of(BetterOnline.MOD_ID, "canfeed"));

    public static final GameRule<Boolean> NO_COOLDOWN =
            GameRuleBuilder.forBoolean(false)
                    .buildAndRegister(Identifier.of(BetterOnline.MOD_ID, "nocooldown"));

    public static final GameRule<Boolean> MOVE =
            GameRuleBuilder.forBoolean(true)
                    .buildAndRegister(Identifier.of(BetterOnline.MOD_ID, "move"));

    public static final GameRule<Boolean> FLY =
            GameRuleBuilder.forBoolean(false)
                    .buildAndRegister(Identifier.of(BetterOnline.MOD_ID, "fly"));

    public static void register() {
        // 静态字段初始化即注册
    }
}
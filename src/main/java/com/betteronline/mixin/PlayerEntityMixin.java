package com.betteronline.mixin;

import com.betteronline.gamerule.ModGameRules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "getAttackCooldownProgress(F)F", at = @At("HEAD"), cancellable = true)
    private void forceFullCooldown(float baseTime, CallbackInfoReturnable<Float> cir) {
        if (shouldForceFull()) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "getAttackCooldownProgressPerTick()F", at = @At("HEAD"), cancellable = true)
    private void forceFullCooldownPerTick(CallbackInfoReturnable<Float> cir) {
        if (shouldForceFull()) {
            cir.setReturnValue(1.0F);
        }
    }

    private boolean shouldForceFull() {
        PlayerEntity player = (PlayerEntity) (Object) this;
        World world = player.getEntityWorld();

        // 服务端：读取自己的 GameRules
        if (world instanceof ServerWorld sw) {
            return sw.getGameRules().getValue(ModGameRules.NO_COOLDOWN);
        }

        // 客户端：从集成服务器（单人/局域网）读取规则
        if (world.isClient()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getServer() != null) {
                return client.getServer().getOverworld()
                        .getGameRules().getValue(ModGameRules.NO_COOLDOWN);
            }
        }

        return false;
    }
}
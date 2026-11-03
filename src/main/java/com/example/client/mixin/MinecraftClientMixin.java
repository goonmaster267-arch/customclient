package com.example.client.mixin;

import com.example.client.util.CombatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    /** Manual left-clicks go through the same weapon-vote pipeline as TriggerBot. */
    @Inject(method = "doAttack", at = @At("HEAD"))
    private void client$preAttack(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        if (mc.player != null && mc.crosshairTarget instanceof EntityHitResult hit) CombatUtil.preAttack(hit.getEntity());
    }
}

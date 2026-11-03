package com.example.client.mixin;

import com.example.client.modules.FullBright;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Overrides only the READ of the gamma option, so options.txt keeps the player's real value. */
@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin<T> {
    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
    private void client$fullbright(CallbackInfoReturnable<T> cir) {
        if (!FullBright.active) return;                      // cheap static check first: this getter is hot
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null && (Object) this == mc.options.getGamma())
            cir.setReturnValue((T) Double.valueOf(FullBright.GAMMA));
    }
}

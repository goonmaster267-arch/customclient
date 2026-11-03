package com.example.client.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("itemUseCooldown") void setItemUseCooldown(int ticks);
    /** The client-side "punched air" delay (10 ticks after a miss). */
    @Accessor("attackCooldown") void setAttackCooldown(int ticks);
    /** The exact routine a real left-click runs (jab for spears, hit for others). */
    @Invoker("doAttack") boolean invokeDoAttack();
}

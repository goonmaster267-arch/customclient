package com.example.client.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {
    /** Vanilla's own slot sync: sends UpdateSelectedSlotC2SPacket and updates lastSelectedSlot. */
    @Invoker("syncSelectedSlot") void invokeSyncSelectedSlot();
}

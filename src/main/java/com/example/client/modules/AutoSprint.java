package com.example.client.modules;

import com.example.client.event.TickEvent;
import com.example.client.module.Category;
import com.example.client.module.Module;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;

/**
 * Runs at START_CLIENT_TICK (before movement is processed) so a hit that cancelled sprint is re-applied
 * before the very next movement tick. Vanilla's tickMovement sends the START_SPRINTING command packet itself.
 */
public final class AutoSprint extends Module {
    public AutoSprint() { super("AutoSprint", Category.MOVEMENT); }

    @Override protected void onEnable() { listen(TickEvent.Pre.class, this::onTick); }

    private void onTick(TickEvent.Pre e) {
        if (!inWorld()) return;
        ClientPlayerEntity p = mc.player;
        if (!mc.options.forwardKey.isPressed() || p.isSprinting()) return;
        if (p.isSneaking() || p.isUsingItem() || p.hasVehicle() || p.horizontalCollision) return;
        if (p.hasStatusEffect(StatusEffects.BLINDNESS)) return;
        if (p.getHungerManager().getFoodLevel() <= 6 && !p.getAbilities().allowFlying) return;
        p.setSprinting(true);
    }
}

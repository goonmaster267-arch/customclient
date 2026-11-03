package com.example.client.modules;

import com.example.client.event.TickEvent;
import com.example.client.mixin.MinecraftClientAccessor;
import com.example.client.module.Category;
import com.example.client.module.Module;
import com.example.client.module.Setting;
import net.minecraft.item.BlockItem;

/** Zeroes the 4-tick right-click delay (MinecraftClient#itemUseCooldown) every tick. */
public final class FastPlace extends Module {
    private final Setting<Boolean> blocksOnly = Setting.bool("BlocksOnly", false);

    public FastPlace() { super("FastPlace", Category.WORLD); addSettings(blocksOnly); }

    @Override protected void onEnable() { listen(TickEvent.Pre.class, this::onTick); }

    private void onTick(TickEvent.Pre e) {
        if (!inWorld()) return;
        if (blocksOnly.bool() && !(mc.player.getMainHandStack().getItem() instanceof BlockItem)) return;
        ((MinecraftClientAccessor) mc).setItemUseCooldown(0);
    }
}

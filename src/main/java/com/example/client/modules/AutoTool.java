package com.example.client.modules;

import com.example.client.event.TickEvent;
import com.example.client.module.Category;
import com.example.client.module.Module;
import com.example.client.util.SwapManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/**
 * While the attack key is held on a block, hold the hotbar tool with the best mining speed for that block.
 * Uses SwapManager (hold = 3 ticks, refreshed each tick) so it auto-reverts a few ticks after mining stops,
 * and it yields to combat swaps: it never steals the slot while a weapon swap is active.
 */
public final class AutoTool extends Module {
    private boolean owns;

    public AutoTool() { super("AutoTool", Category.WORLD); }

    @Override protected void onEnable() { owns = false; listen(TickEvent.Pre.class, this::onTick); }

    private void onTick(TickEvent.Pre e) {
        if (!inWorld()) return;
        if (!SwapManager.isActive()) owns = false;
        ClientPlayerEntity p = mc.player;

        if (!mc.options.attackKey.isPressed() || p.isCreative() || mc.currentScreen != null
                || !(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        if (SwapManager.isActive() && !owns) return;                 // a combat swap currently owns the slot

        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir() || state.getHardness(mc.world, pos) < 0) return;   // air / unbreakable

        PlayerInventory inv = p.getInventory();
        int best = inv.getSelectedSlot();
        float bestSpeed = inv.getStack(best).getMiningSpeedMultiplier(state);
        for (int i = 0; i < 9; i++) {
            float s = inv.getStack(i).getMiningSpeedMultiplier(state);
            if (s > bestSpeed) { bestSpeed = s; best = i; }          // strict '>' keeps the current slot on ties
        }
        if (best != inv.getSelectedSlot() || owns) { if (SwapManager.equip(best, 3)) owns = true; }
    }
}

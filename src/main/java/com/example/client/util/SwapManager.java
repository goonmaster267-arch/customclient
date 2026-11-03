package com.example.client.util;

import com.example.client.event.EventBus;
import com.example.client.event.SlotSwapEvent;
import com.example.client.mixin.ClientPlayerInteractionManagerAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Single owner of all hotbar/inventory swapping (see v1.0 notes):
 *  - hotbar swaps call vanilla's private syncSelectedSlot() via an invoker, so the slot packet is sent and
 *    vanilla's lastSelectedSlot is updated (no duplicate packet next tick, correct order before attack packets);
 *  - main-inventory items use clickSlot(SWAP) into the selected hotbar slot and are reverted with the same click;
 *  - restores are tick-counted, never sleep()/wall-clock.
 * New in 1.1: selectRaw() (immediate, no bookkeeping, used for same-tick swap-and-back) and a suppression flag
 * that silences the weapon-vote pipeline while LungeSwap is mid-sequence.
 */
public final class SwapManager {
    private SwapManager() {}
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static int origSlot = -1;
    private static int invFrom = -1, invHot = -1;
    private static int restoreIn = 0;
    private static volatile boolean suppressed;

    public static boolean isActive() { return origSlot != -1 || invFrom != -1; }
    public static boolean isSuppressed() { return suppressed; }
    public static void setSuppressed(boolean v) { suppressed = v; }

    /**
     * Immediately select a hotbar slot and sync it to the server. No restore bookkeeping: the caller is
     * responsible for selecting back (LungeSwap does so inside the same tick).
     */
    public static void selectRaw(int slot) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || slot < 0 || slot > 8) return;
        PlayerInventory inv = p.getInventory();
        int from = inv.getSelectedSlot();
        if (from == slot) return;
        inv.setSelectedSlot(slot);
        ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
        EventBus.INSTANCE.post(new SlotSwapEvent(from, slot));
    }

    /** Make `slot` (0-35) the held item for at least holdTicks. Idempotent; refreshes the hold timer. */
    public static boolean equip(int slot, int holdTicks) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || slot < 0 || slot > 35) return false;
        PlayerInventory inv = p.getInventory();

        if (slot >= 9) {
            if (p.currentScreenHandler != p.playerScreenHandler) return false;
            if (invFrom != -1) return false;
            int hot = inv.getSelectedSlot();
            if (origSlot == -1) origSlot = hot;
            mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, slot, hot, SlotActionType.SWAP, p);
            invFrom = slot; invHot = hot;
        } else {
            if (origSlot == -1) origSlot = inv.getSelectedSlot();
            selectRaw(slot);
        }
        restoreIn = Math.max(restoreIn, Math.max(1, holdTicks));
        return true;
    }

    public static void scheduleRestore(int ticks) {
        if (isActive()) restoreIn = Math.max(1, Math.min(restoreIn, ticks));
    }

    /** Called from START_CLIENT_TICK. */
    public static void tick() {
        if (isActive() && restoreIn > 0 && --restoreIn == 0) restore();
    }

    private static void restore() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null) { reset(); return; }

        if (invFrom != -1) {
            if (p.currentScreenHandler == p.playerScreenHandler)
                mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, invFrom, invHot, SlotActionType.SWAP, p);
            invFrom = -1; invHot = -1;
        }
        if (origSlot != -1) { selectRaw(origSlot); origSlot = -1; }
        restoreIn = 0;
    }

    public static void reset() { origSlot = -1; invFrom = -1; invHot = -1; restoreIn = 0; suppressed = false; }
}

package com.example.client.event;

import net.minecraft.entity.Entity;

/**
 * Posted immediately BEFORE the attack packet is built. Weapon modules do not swap themselves; they call
 * requestWeapon(priority, slot) and the highest priority wins. This resolves conflicts such as
 * "target has a shield" + "target is heavily armored" + "we are falling" in a deterministic way.
 */
public final class PreAttackEvent extends Event {
    public static final int PRIO_BREACH = 20, PRIO_SHIELD = 25, PRIO_SMASH = 30;

    public final Entity target;
    private int bestPriority = Integer.MIN_VALUE, slot = -1;

    public PreAttackEvent(Entity target) { this.target = target; }

    public void requestWeapon(int priority, int inventorySlot) {
        if (priority > bestPriority) { bestPriority = priority; slot = inventorySlot; }
    }
    /** -1 if nobody asked for a swap. 0-8 hotbar, 9-35 main inventory. */
    public int getSlot() { return slot; }
}

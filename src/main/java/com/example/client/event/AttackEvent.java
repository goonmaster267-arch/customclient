package com.example.client.event;

import net.minecraft.entity.Entity;

/** Fired when the local player attacks an entity (manual OR module-driven). */
public final class AttackEvent extends Event {
    public final Entity target;
    public AttackEvent(Entity target) { this.target = target; }
}

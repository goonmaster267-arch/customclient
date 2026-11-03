package com.example.client.modules;

import com.example.client.event.PreAttackEvent;
import com.example.client.event.TickEvent;
import com.example.client.module.Category;
import com.example.client.module.Module;
import com.example.client.module.Setting;
import com.example.client.util.CombatUtil;
import com.example.client.util.InventoryUtil;
import com.example.client.util.SwapManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;

/**
 * Pre-equips the Mace just before landing and votes for it on every attack while falling > minFall.
 * Getting the height (wind charge, ledge, elytra) is the player's job; this module handles the swap
 * timing, and TriggerBot (if enabled) lands the hit the moment the crosshair is on the target.
 */
public final class MaceStun extends Module {
    private final Setting<Double> minFall = Setting.num("MinFall", 1.5, 0.5, 10);          // smash threshold
    private final Setting<Double> equipDistance = Setting.num("EquipDistance", 4.0, 1, 10); // blocks above ground
    private final Setting<Double> targetRange = Setting.num("TargetRange", 6.0, 2, 12);
    private final Setting<Boolean> needTarget = Setting.bool("NeedTarget", true);
    private final Setting<Boolean> fromInventory = Setting.bool("FromInventory", true);

    public MaceStun() {
        super("MaceStun", Category.COMBAT);
        addSettings(minFall, equipDistance, targetRange, needTarget, fromInventory);
    }

    @Override protected void onEnable() {
        listen(TickEvent.Pre.class, this::onTick);
        listen(PreAttackEvent.class, this::onPreAttack);
    }

    private boolean falling(ClientPlayerEntity p) {
        return !p.isOnGround() && !p.isGliding() && !p.getAbilities().flying && !p.hasVehicle()
                && !p.isTouchingWater() && p.getVelocity().y < 0 && p.fallDistance > minFall.dbl();
    }

    private void onTick(TickEvent.Pre e) {
        if (!inWorld()) return;
        ClientPlayerEntity p = mc.player;
        if (!falling(p)) return;
        double eq = equipDistance.dbl();
        if (CombatUtil.distanceToGround(eq + 1) > eq) return;                 // not close enough to impact yet
        if (needTarget.bool() && !CombatUtil.hasTargetNear(targetRange.dbl())) return;

        int slot = InventoryUtil.find(s -> s.isOf(Items.MACE), fromInventory.bool());
        if (slot >= 0) SwapManager.equip(slot, 10);                           // idempotent: refreshes hold timer
    }

    private void onPreAttack(PreAttackEvent e) {
        if (!inWorld() || !(e.target instanceof LivingEntity)) return;
        ClientPlayerEntity p = mc.player;
        if (p.isOnGround() || p.fallDistance <= minFall.dbl()) return;
        int slot = InventoryUtil.find(s -> s.isOf(Items.MACE), fromInventory.bool());
        if (slot >= 0) e.requestWeapon(PreAttackEvent.PRIO_SMASH, slot);
    }
}

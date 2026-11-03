package com.example.client.util;

import com.example.client.event.EventBus;
import com.example.client.event.PreAttackEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class CombatUtil {
    private CombatUtil() {}
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /**
     * TARGET DETECTION WITHOUT TOUCHING THE RENDER LOOP:
     * mc.crosshairTarget is computed by GameRenderer.updateCrosshairTarget() once per rendered frame, using
     * the player's reach attribute. We only READ that field from the tick thread, which is a plain field
     * read (no world raycast, no partial-tick math, no risk of re-entering rendering code). It can be at most
     * one frame stale, which is identical to what vanilla's own attack key uses. Never call
     * updateCrosshairTarget() yourself from a tick handler.
     */
    public static LivingEntity crosshairLiving() {
        if (mc.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity le && isValidTarget(le)) return le;
        return null;
    }

    public static boolean isValidTarget(LivingEntity e) {
        return e != mc.player && e.isAlive() && e.isAttackable() && !e.isSpectator()
                && !(e instanceof ArmorStandEntity) && !mc.player.isTeammate(e);
    }

    public static boolean hasTargetNear(double range) {
        return !mc.world.getEntitiesByClass(LivingEntity.class,
                mc.player.getBoundingBox().expand(range), CombatUtil::isValidTarget).isEmpty();
    }

    /**
     * Distance from the player's feet down to the first collidable block. Bounded raycast (max blocks), blocks
     * only (no entity scan), executed on the client thread inside a tick, so it is safe and cheap. Unloaded
     * chunks simply return MISS. Returns Double.MAX_VALUE when nothing is within `max`.
     */
    public static double distanceToGround(double max) {
        ClientPlayerEntity p = mc.player;
        Vec3d from = new Vec3d(p.getX(), p.getY(), p.getZ());
        BlockHitResult r = mc.world.raycast(new RaycastContext(from, from.subtract(0, max, 0),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
        return r.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : from.y - r.getPos().y;
    }

    /** 1.21 enchantments are data-driven: resolve the registry entry from the world's registry manager. */
    public static int enchantLevel(RegistryKey<Enchantment> key, ItemStack stack) {
        if (stack.isEmpty() || mc.world == null) return 0;
        return mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT)
                .getOptional(key).map(e -> EnchantmentHelper.getLevel(e, stack)).orElse(0);
    }

    /** Heavy armor = armor points >= threshold, OR any piece with Protection >= protLevel. */
    public static boolean isHeavilyArmored(LivingEntity t, int armorThreshold, int protLevel) {
        if (t.getArmor() >= armorThreshold) return true;
        for (EquipmentSlot s : ARMOR) {
            if (enchantLevel(Enchantments.PROTECTION, t.getEquippedStack(s)) >= protLevel) return true;
        }
        return false;
    }

    /** Shared pre-attack hook: lets weapon modules vote, then performs the winning swap. */
    public static void preAttack(Entity target) {
        if (SwapManager.isSuppressed()) return;   // LungeSwap is mid-sequence: don't let other modules swap
        PreAttackEvent e = EventBus.INSTANCE.post(new PreAttackEvent(target));
        if (e.getSlot() >= 0) SwapManager.equip(e.getSlot(), 2);
    }
}

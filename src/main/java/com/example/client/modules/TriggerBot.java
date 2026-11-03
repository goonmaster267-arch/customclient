package com.example.client.modules;

import com.example.client.event.TickEvent;
import com.example.client.module.Category;
import com.example.client.module.Module;
import com.example.client.module.Setting;
import com.example.client.util.CombatUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

/** Attacks the entity under the crosshair once the swing charge is 1.0, plus an optional delay (+ jitter). */
public final class TriggerBot extends Module {
    private final Setting<Double> delay = Setting.num("Delay", 0, 0, 20);      // extra TICKS after full charge
    private final Setting<Double> jitter = Setting.num("Jitter", 0, 0, 10);    // random 0..N extra ticks
    private final Setting<Boolean> critOnly = Setting.bool("CritOnly", false);
    private final Setting<Boolean> players = Setting.bool("Players", true);
    private final Setting<Boolean> mobs = Setting.bool("Mobs", true);

    /** Remaining delay ticks for the current target; -1 = not armed. Tick counters, never sleep()/wall-clock. */
    private int wait = -1;

    public TriggerBot() {
        super("TriggerBot", Category.COMBAT);
        setKey(GLFW.GLFW_KEY_R);
        addSettings(delay, jitter, critOnly, players, mobs);
    }

    @Override protected void onEnable() { wait = -1; listen(TickEvent.Pre.class, this::onTick); }

    private void onTick(TickEvent.Pre e) {
        if (!inWorld() || mc.currentScreen != null) { wait = -1; return; }
        ClientPlayerEntity p = mc.player;
        if (p.isUsingItem() || p.isSpectator()) { wait = -1; return; }

        LivingEntity t = CombatUtil.crosshairLiving();               // read-only, see CombatUtil javadoc
        if (t == null || !(t instanceof PlayerEntity ? players.bool() : mobs.bool())) { wait = -1; return; }

        // Swing charge: 0.0 = just swung, 1.0 = fully charged. The 0f arg is the "base time" (partial ticks).
        if (p.getAttackCooldownProgress(0f) < 1.0f) return;
        if (critOnly.bool() && !canCrit(p)) return;

        if (wait < 0) wait = delay.intVal() + ThreadLocalRandom.current().nextInt(jitter.intVal() + 1);
        if (wait > 0) { wait--; return; }

        attack(p, t);
        wait = -1;
    }

    /** Vanilla crit conditions (1.9+): falling, airborne, not sprinting, not climbing/swimming/blind/riding. */
    private static boolean canCrit(ClientPlayerEntity p) {
        return p.fallDistance > 0 && !p.isOnGround() && !p.isClimbing() && !p.isTouchingWater()
                && !p.isSprinting() && !p.hasVehicle() && !p.hasStatusEffect(StatusEffects.BLINDNESS);
    }

    private void attack(ClientPlayerEntity p, LivingEntity t) {
        CombatUtil.preAttack(t);                                     // weapon-vote (mace / axe / breach)
        mc.interactionManager.attackEntity(p, t);                    // syncs selected slot, then sends the attack packet
        p.swingHand(Hand.MAIN_HAND);
    }
}

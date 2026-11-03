package com.example.client.modules;

import com.example.client.event.TickEvent;
import com.example.client.module.Category;
import com.example.client.module.Module;
import com.example.client.module.Setting;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.ParticlesMode;

/**
 * Safe, reversible, option-level performance tweaks. It never touches rendering internals, so it cannot
 * conflict with Sodium / Iris / ImmediatelyFast, and everything is restored on disable.
 *
 *  1. Static tweaks: decreased particles, no entity shadows, entity render-distance scale (never RAISES a value
 *     above what the player chose).
 *  2. Adaptive view distance: samples FPS once a second, evaluates every 5 s, moves the view distance by at most
 *     1 chunk per evaluation, then "settles" for 2 evaluations. A change triggers a terrain refresh, so we
 *     deliberately avoid reacting to every dip (hysteresis: shrink below 90% of target, grow above 130%).
 *     It never goes above the player's own setting and adopts manual changes the player makes.
 *  3. Background throttle: caps FPS while the window is unfocused (restores on focus).
 */
public final class FpsBoost extends Module {
    private final Setting<Boolean> adaptive = Setting.bool("AdaptiveViewDistance", true);
    private final Setting<Double> targetFps = Setting.num("TargetFps", 90, 30, 360);
    private final Setting<Double> minView = Setting.num("MinViewDistance", 6, 2, 32);
    private final Setting<Boolean> reduceParticles = Setting.bool("ReduceParticles", true);
    private final Setting<Boolean> noEntityShadows = Setting.bool("NoEntityShadows", true);
    private final Setting<Double> entityScale = Setting.num("EntityDistanceScale", 0.75, 0.5, 5);
    private final Setting<Boolean> throttleBackground = Setting.bool("ThrottleBackground", true);
    private final Setting<Double> backgroundFps = Setting.num("BackgroundFps", 15, 10, 60);

    private Integer origView, applied, origMaxFps;
    private Double origEntity;
    private ParticlesMode origParticles;
    private Boolean origShadows;
    private boolean throttled;
    private int ticks, samples, settle;
    private long fpsSum;

    public FpsBoost() {
        super("FpsBoost", Category.PERFORMANCE);
        addSettings(adaptive, targetFps, minView, reduceParticles, noEntityShadows, entityScale, throttleBackground, backgroundFps);
    }

    @Override protected void onEnable() {
        GameOptions o = mc.options;
        origView = applied = o.getViewDistance().getValue();
        origEntity = o.getEntityDistanceScaling().getValue();
        origParticles = o.getParticles().getValue();
        origShadows = o.getEntityShadows().getValue();
        ticks = samples = settle = 0; fpsSum = 0; throttled = false;

        if (reduceParticles.bool() && origParticles == ParticlesMode.ALL) o.getParticles().setValue(ParticlesMode.DECREASED);
        if (noEntityShadows.bool()) o.getEntityShadows().setValue(false);
        o.getEntityDistanceScaling().setValue(Math.min(origEntity, entityScale.dbl()));
        listen(TickEvent.Pre.class, this::onTick);
    }

    @Override protected void onDisable() {
        GameOptions o = mc.options;
        if (o == null || origView == null) return;
        o.getViewDistance().setValue(origView);
        o.getEntityDistanceScaling().setValue(origEntity);
        o.getParticles().setValue(origParticles);
        o.getEntityShadows().setValue(origShadows);
        if (throttled && origMaxFps != null) o.getMaxFps().setValue(origMaxFps);
        throttled = false;
    }

    private void onTick(TickEvent.Pre e) {
        GameOptions o = mc.options;
        boolean focused = mc.isWindowFocused();

        if (throttleBackground.bool()) {
            if (!focused && !throttled) {
                origMaxFps = o.getMaxFps().getValue();
                o.getMaxFps().setValue(Math.max(10, backgroundFps.intVal()));
                throttled = true;
            } else if (focused && throttled) {
                o.getMaxFps().setValue(origMaxFps);
                throttled = false;
            }
        }

        if (!adaptive.bool() || !focused || mc.currentScreen != null) return;   // menus/alt-tab skew FPS readings
        if (++ticks % 20 == 0) { fpsSum += mc.getCurrentFps(); samples++; }
        if (samples < 5) return;

        int avg = (int) (fpsSum / samples);
        fpsSum = 0; samples = 0;
        if (settle > 0) { settle--; return; }

        int cur = o.getViewDistance().getValue();
        if (cur != applied) { origView = cur; applied = cur; }          // player changed it manually: adopt as new ceiling

        int target = targetFps.intVal(), next = cur;
        if (avg < target * 0.9) next = Math.max(minView.intVal(), cur - 1);
        else if (avg > target * 1.3) next = Math.min(origView, cur + 1);

        if (next != cur) { o.getViewDistance().setValue(next); applied = next; settle = 2; }
    }
}

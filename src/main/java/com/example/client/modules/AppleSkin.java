package com.example.client.modules;

import com.example.client.event.Render2DEvent;
import com.example.client.event.TickEvent;
import com.example.client.module.Category;
import com.example.client.module.Module;
import com.example.client.module.Setting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import java.util.Locale;

/**
 * AppleSkin-style food HUD, drawn with plain fills + text (no texture/pipeline calls) so it survives the
 * 1.21.6+ GUI rendering rewrite:
 *  - gold bar above the hunger bar = saturation (right-aligned like the hunger icons);
 *  - pulsing green block over the hunger icons = hunger the held food would restore;
 *  - translucent gold = saturation after eating; label with the exact numbers.
 * All numbers are computed once per tick; the per-frame path only draws.
 */
public final class AppleSkin extends Module {
    private final Setting<Boolean> showNumbers = Setting.bool("ShowNumbers", true);

    private int food, gain;
    private float sat, newSat;
    private boolean hasFood, visible;
    private String label = "";

    public AppleSkin() { super("AppleSkin", Category.HUD); addSettings(showNumbers); }

    @Override protected void onEnable() {
        listen(TickEvent.Post.class, this::onTick);
        listen(Render2DEvent.class, this::onRender);
    }

    private void onTick(TickEvent.Post e) {
        if (!inWorld()) { visible = false; return; }
        ClientPlayerEntity p = mc.player;
        // Vanilla replaces the food bar with the mount's health bar while riding a living mount.
        visible = mc.interactionManager.hasStatusBars() && !(p.getVehicle() instanceof LivingEntity);
        if (!visible) return;

        food = p.getHungerManager().getFoodLevel();
        sat = p.getHungerManager().getSaturationLevel();

        FoodComponent fc = foodOf(p.getMainHandStack());
        if (fc == null) fc = foodOf(p.getOffHandStack());
        hasFood = fc != null && (food < 20 || fc.canAlwaysEat());
        if (hasFood) {
            gain = Math.min(fc.nutrition(), 20 - food);
            newSat = Math.min(sat + fc.saturation(), (float) (food + gain));
            label = String.format(Locale.ROOT, "+%d hunger  +%.1f sat", gain, newSat - sat);
        }
    }

    private static FoodComponent foodOf(ItemStack s) { return s.isEmpty() ? null : s.get(DataComponentTypes.FOOD); }

    private void onRender(Render2DEvent e) {
        if (!visible || mc.options.hudHidden) return;
        DrawContext g = e.context;
        int right = g.getScaledWindowWidth() / 2 + 91;       // right edge of the vanilla food bar
        int top = g.getScaledWindowHeight() - 39;            // top of the food icons
        int px = 4;                                          // 10 icons * 8px = 4px per hunger point

        if (hasFood) {
            int alpha = 0x60 + (int) (0x50 * (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 200.0)));
            int foodW = food * px;
            g.fill(right - foodW - gain * px, top, right - foodW, top + 9, (alpha << 24) | 0x55FF55);
            g.fill(right - Math.round(80f * Math.min(newSat, 20f) / 20f), top - 3, right, top - 1, 0x80FFC832);
        }
        g.fill(right - Math.round(80f * Math.min(sat, 20f) / 20f), top - 3, right, top - 1, 0xFFFFC832);

        if (showNumbers.bool() && hasFood) {
            TextRenderer tr = mc.textRenderer;
            g.drawTextWithShadow(tr, label, right - tr.getWidth(label), top - 14, 0xFFFFFFFF);
        }
    }
}

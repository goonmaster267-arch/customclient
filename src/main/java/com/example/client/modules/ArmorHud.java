package com.example.client.modules;

import com.example.client.event.Render2DEvent;
import com.example.client.event.TickEvent;
import com.example.client.module.Category;
import com.example.client.module.Module;
import com.example.client.module.Setting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.Arrays;

/**
 * Armor + held-tool durability HUD: item icon, vanilla durability bar, and a colored percentage.
 * Strings/colors are rebuilt only when a stack's remaining durability actually changes (tick thread);
 * the render path only draws. Optional one-shot low-durability warning on the action bar.
 */
public final class ArmorHud extends Module {
    private static final EquipmentSlot[] ORDER = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    private final Setting<Boolean> showHands = Setting.bool("ShowHands", true);
    private final Setting<Boolean> rawNumbers = Setting.bool("RawNumbers", false);
    private final Setting<Boolean> warn = Setting.bool("LowWarning", true);
    private final Setting<Double> warnPercent = Setting.num("WarnPercent", 10, 1, 50);

    private final ItemStack[] stacks = new ItemStack[ORDER.length];
    private final String[] labels = new String[ORDER.length];
    private final int[] colors = new int[ORDER.length];
    private final int[] lastLeft = new int[ORDER.length];
    private final boolean[] warned = new boolean[ORDER.length];

    public ArmorHud() {
        super("ArmorHud", Category.HUD);
        addSettings(showHands, rawNumbers, warn, warnPercent);
        Arrays.fill(stacks, ItemStack.EMPTY);
        Arrays.fill(lastLeft, -1);
    }

    @Override protected void onEnable() {
        listen(TickEvent.Post.class, this::onTick);
        listen(Render2DEvent.class, this::onRender);
    }

    private void onTick(TickEvent.Post e) {
        if (!inWorld()) return;
        ClientPlayerEntity p = mc.player;
        for (int i = 0; i < ORDER.length; i++) {
            ItemStack s = (i >= 4 && !showHands.bool()) ? ItemStack.EMPTY : p.getEquippedStack(ORDER[i]);
            if (s.isEmpty() || !s.isDamageable()) { stacks[i] = ItemStack.EMPTY; warned[i] = false; lastLeft[i] = -1; continue; }

            int max = s.getMaxDamage(), left = max - s.getDamage();
            int pct = max <= 0 ? 0 : (int) ((left * 100L) / max);
            stacks[i] = s;
            if (left != lastLeft[i]) {                                   // rebuild strings only on change
                lastLeft[i] = left;
                labels[i] = rawNumbers.bool() ? left + "/" + max : pct + "%";
                colors[i] = pct > 50 ? 0xFF55FF55 : pct > 25 ? 0xFFFFFF55 : pct > 10 ? 0xFFFFAA00 : 0xFFFF5555;
            }
            if (warn.bool() && pct <= warnPercent.dbl() && !warned[i]) {
                warned[i] = true;
                p.sendMessage(Text.literal("[!] " + s.getName().getString() + " at " + pct + "%"), true);
            } else if (pct > warnPercent.dbl() + 5) warned[i] = false;     // repaired / mended
        }
    }

    private void onRender(Render2DEvent e) {
        if (mc.player == null || mc.options.hudHidden) return;
        int count = 0;
        for (ItemStack s : stacks) if (!s.isEmpty()) count++;
        if (count == 0) return;

        DrawContext g = e.context;
        TextRenderer tr = mc.textRenderer;
        int x = 4, y = g.getScaledWindowHeight() / 2 - count * 9;
        for (int i = 0; i < stacks.length; i++) {
            if (stacks[i].isEmpty()) continue;
            g.drawItem(stacks[i], x, y);
            g.drawStackOverlay(tr, stacks[i], x, y);                       // vanilla durability bar
            g.drawTextWithShadow(tr, labels[i], x + 20, y + 5, colors[i]);
            y += 18;
        }
    }
}

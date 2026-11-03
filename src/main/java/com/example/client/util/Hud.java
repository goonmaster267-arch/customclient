package com.example.client.util;

import com.example.client.event.Render2DEvent;
import com.example.client.module.Module;
import com.example.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Active-module list. The list is rebuilt from the tick thread (rarely); the per-frame path only draws. */
public final class Hud {
    private Hud() {}
    private static volatile String[] names = new String[0];

    public static void refresh() {
        List<String> out = new ArrayList<>();
        for (Module m : ModuleManager.INSTANCE.getModules())
            if (m.isEnabled() && m.getCategory() != com.example.client.module.Category.HUD
                    && m.getCategory() != com.example.client.module.Category.PERFORMANCE) out.add(m.getName());
        names = out.toArray(new String[0]);
    }

    public static void render(Render2DEvent e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        String[] list = names;
        if (list.length == 0) return;
        DrawContext g = e.context;
        TextRenderer tr = mc.textRenderer;
        int w = g.getScaledWindowWidth(), y = 2;
        for (String n : list) { g.drawTextWithShadow(tr, n, w - tr.getWidth(n) - 2, y, 0xFF55FFFF); y += 10; }
    }
}

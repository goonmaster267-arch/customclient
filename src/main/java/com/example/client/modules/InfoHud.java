package com.example.client.modules;

import com.example.client.event.Render2DEvent;
import com.example.client.event.TickEvent;
import com.example.client.module.Category;
import com.example.client.module.Module;
import com.example.client.module.Setting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;

/** FPS / coordinates / facing / ping, refreshed 4x per second on the tick thread; drawing is allocation-free. */
public final class InfoHud extends Module {
    private final Setting<Boolean> showFps = Setting.bool("FPS", true);
    private final Setting<Boolean> showCoords = Setting.bool("Coords", true);
    private final Setting<Boolean> showFacing = Setting.bool("Facing", true);
    private final Setting<Boolean> showPing = Setting.bool("Ping", true);

    private String fps = "", pos = "", facing = "", ping = "";
    private int fpsColor = 0xFF55FF55, tick;

    public InfoHud() { super("InfoHud", Category.HUD); addSettings(showFps, showCoords, showFacing, showPing); }

    @Override protected void onEnable() {
        listen(TickEvent.Post.class, this::onTick);
        listen(Render2DEvent.class, this::onRender);
    }

    private void onTick(TickEvent.Post e) {
        if (!inWorld() || tick++ % 5 != 0) return;
        ClientPlayerEntity p = mc.player;
        int f = mc.getCurrentFps();
        fps = "FPS: " + f;
        fpsColor = f >= 60 ? 0xFF55FF55 : f >= 30 ? 0xFFFFFF55 : 0xFFFF5555;
        pos = "XYZ: " + (int) Math.floor(p.getX()) + " " + (int) Math.floor(p.getY()) + " " + (int) Math.floor(p.getZ());
        facing = "Facing: " + p.getHorizontalFacing().asString();
        PlayerListEntry entry = mc.getNetworkHandler() == null ? null : mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
        ping = entry == null ? "" : "Ping: " + entry.getLatency() + "ms";
    }

    private void onRender(Render2DEvent e) {
        if (mc.player == null || mc.options.hudHidden) return;
        DrawContext g = e.context;
        TextRenderer tr = mc.textRenderer;
        int y = 4;
        if (showFps.bool() && !fps.isEmpty())    { g.drawTextWithShadow(tr, fps, 4, y, fpsColor); y += 10; }
        if (showCoords.bool() && !pos.isEmpty()) { g.drawTextWithShadow(tr, pos, 4, y, 0xFFFFFFFF); y += 10; }
        if (showFacing.bool() && !facing.isEmpty()) { g.drawTextWithShadow(tr, facing, 4, y, 0xFFFFFFFF); y += 10; }
        if (showPing.bool() && !ping.isEmpty())  { g.drawTextWithShadow(tr, ping, 4, y, 0xFFAAAAAA); }
    }
}

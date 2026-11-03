package com.example.client.event;

import net.minecraft.client.gui.DrawContext;

/** Re-used every frame: the HUD path allocates nothing. `context` is only valid during the post() call. */
public final class Render2DEvent extends Event {
    public static final Render2DEvent INSTANCE = new Render2DEvent();
    public DrawContext context;
    private Render2DEvent() {}
}

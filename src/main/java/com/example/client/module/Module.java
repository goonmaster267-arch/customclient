package com.example.client.module;

import com.example.client.ClientMod;
import com.example.client.event.Event;
import com.example.client.event.EventBus;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Base module. Subclasses register their event listeners in onEnable() through listen(...); every
 * subscription is tracked and removed automatically on disable, so a disabled module costs exactly zero
 * per tick (no "if (!enabled) return" checks scattered around).
 */
public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final Category category;
    private int key = GLFW.GLFW_KEY_UNKNOWN;
    private boolean enabled;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<EventBus.Subscription> subscriptions = new ArrayList<>();

    protected Module(String name, Category category) { this.name = name; this.category = category; }

    protected void addSettings(Setting<?>... s) { settings.addAll(List.of(s)); }

    protected <T extends Event> void listen(Class<T> type, Consumer<T> handler) { listen(type, 0, handler); }

    protected <T extends Event> void listen(Class<T> type, int priority, Consumer<T> handler) {
        subscriptions.add(EventBus.INSTANCE.subscribe(type, priority, handler));
    }

    protected void onEnable() {}
    protected void onDisable() {}

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean on) {
        if (on == enabled) return;
        enabled = on;
        try {
            if (on) onEnable();
            else { unsubscribeAll(); onDisable(); }
        } catch (Throwable t) {
            ClientMod.LOGGER.error("Module {} failed to {}", name, on ? "enable" : "disable", t);
            enabled = false;
            unsubscribeAll();
        }
    }

    private void unsubscribeAll() { subscriptions.forEach(EventBus.Subscription::unsubscribe); subscriptions.clear(); }

    protected static boolean inWorld() { return mc.player != null && mc.world != null && mc.interactionManager != null; }

    public String getName() { return name; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }
    public List<Setting<?>> getSettings() { return settings; }
}

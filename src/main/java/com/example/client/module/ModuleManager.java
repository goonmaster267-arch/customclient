package com.example.client.module;

import com.example.client.event.EventBus;
import com.example.client.event.KeyEvent;
import com.example.client.event.TickEvent;
import com.example.client.modules.*;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public final class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();
    private final List<Module> modules = new ArrayList<>();
    private final Map<Integer, Boolean> keyDown = new HashMap<>();

    public void init() {
        register(new TriggerBot(), new MaceStun(), new BreachSwap(), new ShieldBreaker(),
                 new LungeSwap(), new AutoSprint(), new FullBright(), new FastPlace(), new AutoTool(),
                 new FpsBoost(), new AppleSkin(), new ArmorHud(), new InfoHud());

        // Fabric has no raw-key callback, so we poll ONLY the keys that are bound to modules and emit
        // edge-triggered KeyEvents. (For arbitrary keys, add a mixin on net.minecraft.client.Keyboard#onKey.)
        EventBus.INSTANCE.subscribe(TickEvent.Pre.class, 1000, e -> pollKeys());
        EventBus.INSTANCE.subscribe(KeyEvent.class, 0, e -> {
            if (!e.pressed) return;
            for (Module m : modules) if (m.getKey() == e.key) m.toggle();
        });
    }

    private boolean defaultsApplied;

    /** Always-on modules, enabled once on first world join. */
    public void enableDefaults() {
        if (defaultsApplied) return;
        defaultsApplied = true;
        List<Class<? extends Module>> defaults = List.of(FpsBoost.class, AppleSkin.class, ArmorHud.class, InfoHud.class);
        for (Class<? extends Module> c : defaults) get(c).setEnabled(true);
    }

    private void register(Module... ms) { modules.addAll(Arrays.asList(ms)); }

    private void pollKeys() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null) return;              // never toggle while typing in chat / GUIs
        long window = mc.getWindow().getHandle();
        for (Module m : modules) {
            int k = m.getKey();
            if (k < 0) continue;
            boolean down = GLFW.glfwGetKey(window, k) == GLFW.GLFW_PRESS;   // raw GLFW: stable across 1.21.9+ input rewrite
            if (down != keyDown.getOrDefault(k, false)) {
                keyDown.put(k, down);
                EventBus.INSTANCE.post(new KeyEvent(k, down));
            }
        }
    }

    public List<Module> getModules() { return Collections.unmodifiableList(modules); }

    public <T extends Module> T get(Class<T> type) {
        for (Module m : modules) if (type.isInstance(m)) return type.cast(m);
        throw new NoSuchElementException(type.getSimpleName());
    }
}

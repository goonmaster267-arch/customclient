package com.example.client;

import com.example.client.event.*;
import com.example.client.module.ModuleManager;
import com.example.client.util.Hud;
import com.example.client.util.SwapManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientMod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("CustomClient");
    private static float lastCharge = 1f;
    private static int hudTick;

    @Override
    public void onInitializeClient() {
        EventBus bus = EventBus.INSTANCE;
        ModuleManager.INSTANCE.init();

        bus.subscribe(Render2DEvent.class, 0, Hud::render);
        bus.subscribe(AttackEvent.class, 0, e -> { if (!SwapManager.isSuppressed()) SwapManager.scheduleRestore(1); });

        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.world == null) return;
            SwapManager.tick();
            bus.post(TickEvent.Pre.INSTANCE);
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.world == null) { lastCharge = 1f; return; }
            bus.post(TickEvent.Post.INSTANCE);
            if (++hudTick % 10 == 0) Hud.refresh();
            float charge = mc.player.getAttackCooldownProgress(0f);
            if (charge >= 1f && lastCharge < 1f) bus.post(new CooldownReadyEvent());
            lastCharge = charge;
        });

        // 1.21.11 HUD API (HudRenderCallback is deprecated since Fabric API 0.116). One element, drawn just
        // before chat, feeds the whole Render2DEvent pipeline. The event object is reused every frame.
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.of("customclient", "overlay"),
                (ctx, tickCounter) -> {
                    Render2DEvent r = Render2DEvent.INSTANCE;
                    r.context = ctx;
                    try { bus.post(r); } finally { r.context = null; }
                });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (player == MinecraftClient.getInstance().player) bus.post(new AttackEvent(entity));
            return ActionResult.PASS;
        });

        // Options are guaranteed to exist once we are in a world: enable the always-on QoL/perf modules then.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> ModuleManager.INSTANCE.enableDefaults());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> SwapManager.reset());
        LOGGER.info("CustomClient loaded");
    }
}

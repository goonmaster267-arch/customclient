package com.example.client.modules;

import com.example.client.event.TickEvent;
import com.example.client.mixin.MinecraftClientAccessor;
import com.example.client.module.Category;
import com.example.client.module.Module;
import com.example.client.module.Setting;
import com.example.client.util.CombatUtil;
import com.example.client.util.SwapManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import org.lwjgl.glfw.GLFW;

/**
 * Auto Lunge Swap (1.21.11 spears).
 *
 * Toggle key: O.  Lunge key (tap = one lunge, hold = chain): V.
 *
 * WHY THE SWAP: a spear jab needs a fully charged attack meter, and the meter recharges according to the
 * attack_speed ATTRIBUTE of what you were holding. Attributes are refreshed from equipment once per tick, so a
 * jab done in the same tick as the item switch is judged with the attributes of the item you held BEFORE the
 * switch, while the held stack (and its Lunge enchantment) is already the spear. If we swap spear -> carrier
 * again inside that same tick, the held item never "changes" from the tick logic's point of view, so the
 * cooldown is not reset. Whatever carrier you idle on therefore sets the lunge rate: empty hand ~5 ticks,
 * sword ~12-13, mace ~33. By default we park on an empty hotbar slot (fastest).
 *
 * SEQUENCE per lunge (all in ONE client tick, packets in order):
 *   [select spear + slot packet] -> [doAttack(): same routine as a real left click] -> [select carrier + slot packet]
 * Parking onto the carrier happens one tick EARLIER (attributes must refresh while on the carrier), which is
 * why the first lunge after pressing the key has a 1-tick lead-in when you were holding something else.
 *
 * Lunge rules (vanilla): needs food level > 6, does not work in water or while gliding, costs exhaustion
 * (4 x level), and does not fire while you are eating / charging / using an item.
 *
 * NOTE: this relies on a vanilla attribute-swap bug (MC-28289). Mojang intends to fix it, so it can stop working
 * in later versions, and most PvP servers treat swap macros as cheating.
 */
public final class LungeSwap extends Module {
    private final Setting<Boolean> emptyCarrier = Setting.bool("EmptyCarrier", true);   // park on an empty hotbar slot
    private final Setting<Boolean> holdToRepeat = Setting.bool("HoldToRepeat", true);
    private final Setting<Double> minHunger = Setting.num("MinHunger", 7, 1, 20);       // vanilla needs > 6
    private final Setting<Double> inputBuffer = Setting.num("InputBuffer", 3, 1, 10);   // early taps remembered (ticks)
    private final Setting<Double> extraGap = Setting.num("ExtraGapTicks", 0, 0, 20);
    private final Setting<Boolean> ignoreMissDelay = Setting.bool("IgnoreMissDelay", false); // clears the 10-tick air-punch delay

    private int lungeKey = GLFW.GLFW_KEY_V;
    private boolean wasDown;
    private int buffer, gap;
    private boolean parked;

    public LungeSwap() {
        super("LungeSwap", Category.COMBAT);
        setKey(GLFW.GLFW_KEY_O);
        addSettings(emptyCarrier, holdToRepeat, minHunger, inputBuffer, extraGap, ignoreMissDelay);
    }

    public void setLungeKey(int glfwKey) { this.lungeKey = glfwKey; }

    @Override protected void onEnable() {
        wasDown = false; buffer = 0; gap = 0; parked = false;
        listen(TickEvent.Pre.class, this::onTick);
    }

    @Override protected void onDisable() {
        if (parked) SwapManager.scheduleRestore(1);   // hand the original item back
        parked = false;
    }

    private void onTick(TickEvent.Pre e) {
        if (!inWorld()) return;
        ClientPlayerEntity p = mc.player;

        boolean down = mc.currentScreen == null
                && GLFW.glfwGetKey(mc.getWindow().getHandle(), lungeKey) == GLFW.GLFW_PRESS;
        if (down && !wasDown) buffer = inputBuffer.intVal();        // a tap is remembered for a few ticks
        wasDown = down;

        boolean want = (down && holdToRepeat.bool()) || buffer > 0;
        if (buffer > 0) buffer--;

        if (!want) {                                                  // key released: give the item back
            if (parked) { SwapManager.scheduleRestore(1); parked = false; }
            return;
        }

        // Vanilla lunge preconditions. Never interrupt eating / drinking / charging.
        if (mc.currentScreen != null || p.isUsingItem() || p.isSpectator() || p.isTouchingWater() || p.isGliding()) return;
        if (p.getHungerManager().getFoodLevel() < minHunger.intVal()) return;

        PlayerInventory inv = p.getInventory();
        int spear = findLungeSpear(inv);
        if (spear < 0) return;

        int cur = inv.getSelectedSlot();
        int carrier = emptyCarrier.bool() ? findEmptyHotbarSlot(inv, cur) : cur;

        // Step 1: be on the carrier for a full tick so attributes are the carrier's.
        SwapManager.equip(carrier, 3);                                // idempotent, refreshes hold timer
        parked = SwapManager.isActive();
        if (cur != carrier) { buffer = Math.max(buffer, 3); return; } // keep the tap alive during the lead-in tick

        if (gap > 0) { gap--; return; }
        if (p.getAttackCooldownProgress(0f) < 1.0f) return;           // jab only accepted at full charge

        fire(spear, carrier);
        buffer = 0;
        gap = extraGap.intVal();
    }

    /** spear -> jab -> carrier, inside one tick. While suppressed, other weapon modules stay out of it. */
    private void fire(int spear, int carrier) {
        SwapManager.setSuppressed(true);
        try {
            if (spear != carrier) SwapManager.selectRaw(spear);
            MinecraftClientAccessor acc = (MinecraftClientAccessor) mc;
            if (ignoreMissDelay.bool()) acc.setAttackCooldown(0);
            acc.invokeDoAttack();                                     // identical to a real left click
        } finally {
            if (spear != carrier) SwapManager.selectRaw(carrier);     // MUST happen in this same tick
            SwapManager.setSuppressed(false);
        }
    }

    /** Hotbar slot holding the spear with the highest Lunge level (Lunge is spear-exclusive), or -1. */
    private static int findLungeSpear(PlayerInventory inv) {
        int best = -1, bestLevel = 0;
        for (int i = 0; i < 9; i++) {
            int lvl = CombatUtil.enchantLevel(Enchantments.LUNGE, inv.getStack(i));
            if (lvl > bestLevel) { bestLevel = lvl; best = i; }
        }
        return best;
    }

    /** Prefer the slot we are already on if it is empty; else any empty hotbar slot; else stay put. */
    private static int findEmptyHotbarSlot(PlayerInventory inv, int cur) {
        if (inv.getStack(cur).isEmpty()) return cur;
        for (int i = 0; i < 9; i++) if (inv.getStack(i).isEmpty()) return i;
        return cur;
    }
}

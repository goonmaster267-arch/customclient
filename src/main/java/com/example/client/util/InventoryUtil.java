package com.example.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import java.util.function.Predicate;

public final class InventoryUtil {
    private InventoryUtil() {}

    /**
     * Returns the PlayerInventory index (0-8 hotbar, 9-35 main inventory) of the first stack matching p,
     * or -1. The currently selected slot is checked first so we never swap away from a weapon we already hold.
     */
    public static int find(Predicate<ItemStack> p, boolean includeMainInventory) {
        PlayerInventory inv = MinecraftClient.getInstance().player.getInventory();
        if (p.test(inv.getStack(inv.getSelectedSlot()))) return inv.getSelectedSlot();
        for (int i = 0; i < 9; i++) if (p.test(inv.getStack(i))) return i;
        if (includeMainInventory) for (int i = 9; i < 36; i++) if (p.test(inv.getStack(i))) return i;
        return -1;
    }
}

package com.example.client.modules;

import com.example.client.event.PreAttackEvent;
import com.example.client.module.Category;
import com.example.client.module.Module;
import com.example.client.module.Setting;
import com.example.client.util.InventoryUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.ItemTags;

/** Target is blocking -> vote for an axe (axes disable shields). Primary weapon is restored after the hit. */
public final class ShieldBreaker extends Module {
    private final Setting<Boolean> playersOnly = Setting.bool("PlayersOnly", false);
    private final Setting<Boolean> fromInventory = Setting.bool("FromInventory", true);

    public ShieldBreaker() {
        super("ShieldBreaker", Category.COMBAT);
        addSettings(playersOnly, fromInventory);
    }

    @Override protected void onEnable() { listen(PreAttackEvent.class, this::onPreAttack); }

    private void onPreAttack(PreAttackEvent e) {
        if (!inWorld() || !(e.target instanceof LivingEntity t) || !t.isBlocking()) return;
        if (playersOnly.bool() && !(t instanceof PlayerEntity)) return;
        int slot = InventoryUtil.find(s -> s.isIn(ItemTags.AXES), fromInventory.bool());
        if (slot >= 0) e.requestWeapon(PreAttackEvent.PRIO_SHIELD, slot);
    }
}

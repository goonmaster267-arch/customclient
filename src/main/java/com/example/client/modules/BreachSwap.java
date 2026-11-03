package com.example.client.modules;

import com.example.client.event.PreAttackEvent;
import com.example.client.module.Category;
import com.example.client.module.Module;
import com.example.client.module.Setting;
import com.example.client.util.CombatUtil;
import com.example.client.util.InventoryUtil;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;

/** Heavily armored target -> vote for a Breach mace. SwapManager returns to the primary weapon afterwards. */
public final class BreachSwap extends Module {
    private final Setting<Double> armorThreshold = Setting.num("ArmorPoints", 15, 1, 20);
    private final Setting<Double> protLevel = Setting.num("ProtLevel", 4, 1, 4);
    private final Setting<Boolean> fromInventory = Setting.bool("FromInventory", true);

    public BreachSwap() {
        super("BreachSwap", Category.COMBAT);
        addSettings(armorThreshold, protLevel, fromInventory);
    }

    @Override protected void onEnable() { listen(PreAttackEvent.class, this::onPreAttack); }

    private void onPreAttack(PreAttackEvent e) {
        if (!inWorld() || !(e.target instanceof LivingEntity t)) return;
        if (!CombatUtil.isHeavilyArmored(t, armorThreshold.intVal(), protLevel.intVal())) return;
        int slot = InventoryUtil.find(
                s -> s.isOf(Items.MACE) && CombatUtil.enchantLevel(Enchantments.BREACH, s) > 0, fromInventory.bool());
        if (slot >= 0) e.requestWeapon(PreAttackEvent.PRIO_BREACH, slot);
    }
}

package com.example.client.modules;

import com.example.client.module.Category;
import com.example.client.module.Module;

/** The actual work is in SimpleOptionMixin, which overrides the gamma getter while `active` is true. */
public final class FullBright extends Module {
    public static volatile boolean active;
    public static final double GAMMA = 16.0;

    public FullBright() { super("FullBright", Category.RENDER); }

    @Override protected void onEnable() { active = true; }
    @Override protected void onDisable() { active = false; }
}

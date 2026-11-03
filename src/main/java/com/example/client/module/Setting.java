package com.example.client.module;

/** Minimal typed setting. Numbers are stored as Double and clamped on set(). */
public final class Setting<T> {
    public final String name;
    private T value;
    private final double min, max;

    private Setting(String name, T def, double min, double max) {
        this.name = name; this.value = def; this.min = min; this.max = max;
    }

    public static Setting<Boolean> bool(String name, boolean def) { return new Setting<>(name, def, 0, 1); }
    public static Setting<Double> num(String name, double def, double min, double max) { return new Setting<>(name, def, min, max); }

    public T get() { return value; }

    @SuppressWarnings("unchecked")
    public void set(T v) {
        if (v instanceof Double d) v = (T) Double.valueOf(Math.clamp(d, min, max));
        value = v;
    }

    public boolean bool() { return (Boolean) value; }
    public double dbl() { return ((Number) value).doubleValue(); }
    public int intVal() { return (int) Math.round(dbl()); }
}

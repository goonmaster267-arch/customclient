package com.example.client.event;

public final class KeyEvent extends Event {
    public final int key; public final boolean pressed;
    public KeyEvent(int key, boolean pressed) { this.key = key; this.pressed = pressed; }
}

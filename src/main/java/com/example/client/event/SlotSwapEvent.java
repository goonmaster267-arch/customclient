package com.example.client.event;

public final class SlotSwapEvent extends Event {
    public final int from, to;
    public SlotSwapEvent(int from, int to) { this.from = from; this.to = to; }
}

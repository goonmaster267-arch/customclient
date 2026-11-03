package com.example.client.event;

import com.example.client.ClientMod;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Lightweight, allocation-free-on-post event bus.
 *  - Listener arrays are copy-on-write: subscribe/unsubscribe are rare, post() is hot.
 *  - post() iterates a plain array (no iterator, no locking).
 *  - Every handler is wrapped in try/catch. This matters most for Render2DEvent: an exception thrown
 *    from a HUD listener would otherwise propagate into the render loop and crash the game every frame.
 *    A listener that throws MAX_ERRORS times is auto-unsubscribed instead of spamming the log at 144 fps.
 */
public final class EventBus {
    public static final EventBus INSTANCE = new EventBus();
    private static final int MAX_ERRORS = 5;

    private static final class Listener<T> {
        final Consumer<T> handler;
        final int priority;
        int errors;
        Listener(Consumer<T> handler, int priority) { this.handler = handler; this.priority = priority; }
    }

    public static final class Subscription {
        private final EventBus bus; private final Class<?> type; private final Listener<?> listener;
        private Subscription(EventBus bus, Class<?> type, Listener<?> l) { this.bus = bus; this.type = type; this.listener = l; }
        public void unsubscribe() { bus.remove(type, listener); }
    }

    private final Map<Class<?>, Listener<?>[]> listeners = new ConcurrentHashMap<>();

    /** Higher priority runs first. Equal priorities keep registration order. */
    public <T extends Event> Subscription subscribe(Class<T> type, int priority, Consumer<T> handler) {
        Listener<T> l = new Listener<>(handler, priority);
        listeners.merge(type, new Listener<?>[]{l}, (old, ignored) -> {
            Listener<?>[] n = Arrays.copyOf(old, old.length + 1);
            n[old.length] = l;
            Arrays.sort(n, Comparator.comparingInt((Listener<?> x) -> x.priority).reversed());
            return n;
        });
        return new Subscription(this, type, l);
    }

    private void remove(Class<?> type, Listener<?> l) {
        listeners.computeIfPresent(type, (k, arr) -> {
            Listener<?>[] n = Arrays.stream(arr).filter(x -> x != l).toArray(Listener<?>[]::new);
            return n.length == 0 ? null : n;
        });
    }

    @SuppressWarnings("unchecked")
    public <E extends Event> E post(E event) {
        Listener<?>[] arr = listeners.get(event.getClass());
        if (arr == null) return event;
        for (Listener<?> raw : arr) {
            if (event instanceof CancellableEvent c && c.isCancelled()) break;
            Listener<E> l = (Listener<E>) raw;
            try {
                l.handler.accept(event);
            } catch (Throwable t) {
                if (++l.errors >= MAX_ERRORS) {
                    ClientMod.LOGGER.error("Listener for {} disabled after {} errors", event.getClass().getSimpleName(), MAX_ERRORS, t);
                    remove(event.getClass(), l);
                } else {
                    ClientMod.LOGGER.error("Listener error in {}", event.getClass().getSimpleName(), t);
                }
            }
        }
        return event;
    }
}

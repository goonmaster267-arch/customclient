package com.example.client.event;

/** Singletons: posting a tick event allocates nothing. Pre = START_CLIENT_TICK, Post = END_CLIENT_TICK. */
public final class TickEvent {
    private TickEvent() {}
    public static final class Pre extends Event { public static final Pre INSTANCE = new Pre(); private Pre() {} }
    public static final class Post extends Event { public static final Post INSTANCE = new Post(); private Post() {} }
}

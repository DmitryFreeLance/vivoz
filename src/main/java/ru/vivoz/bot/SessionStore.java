package ru.vivoz.bot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStore {
    private final Map<Long, Session> sessions = new ConcurrentHashMap<>();

    public Session get(long userId) {
        return sessions.get(userId);
    }

    public void put(long userId, Session session) {
        sessions.put(userId, session);
    }

    public void clear(long userId) {
        sessions.remove(userId);
    }
}

package org.jumpserver.chen.framework.session;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class SessionManager {
    // Bind the Servlet HTTP session that created the Chen session; used during the WS handshake to prevent the token from being replayed across browsers.
    public static final String WEB_SESSION_ID_ATTRIBUTE = "webSessionId";
    private final static SessionManager instance = new SessionManager();
    private final static ThreadLocal<String> token = new ThreadLocal<>();
    private final Map<String, Session> store = new ConcurrentHashMap<>();
    // Each Chen session can only have one primary /ws/session connection; the value is the WebSocket session id.
    private final Map<String, String> primaryWebSockets = new ConcurrentHashMap<>();

    public static String registerSession(Session session) {
        String token = createToken();
        session.setWebToken(token);
        instance.store.put(token, session);
        log.info("new session created, current session count {}", instance.getCurrentSessionCount());
        return token;
    }

    public static void unregisterSession(String token) {
        instance.store.remove(token);
        instance.primaryWebSockets.remove(token);
        log.info("session {} unregistered, current session count {}", token, instance.getCurrentSessionCount());
    }

    public int getCurrentSessionCount() {
        return instance.store.size();
    }

    public static void setContext(String token) {
        SessionManager.token.set(token);
    }

    public static String getContextToken() {
        return token.get();
    }

    public static SessionManager getInstance() {
        return instance;
    }

    public static Session getCurrentSession() {
        return instance.store.get(token.get());
    }

    public static Map<String, Session> getStore() {
        return instance.store;
    }

    public static Session getSession(String token) {
        return instance.store.get(token);
    }

    public static boolean claimPrimaryWebSocket(String token, String webSocketId) {
        // Atomic claim, to avoid concurrent handshakes simultaneously replacing the current primary connection.
        String existing = instance.primaryWebSockets.putIfAbsent(token, webSocketId);
        return existing == null || existing.equals(webSocketId);
    }

    public static boolean releasePrimaryWebSocket(String token, String webSocketId) {
        // Only the claimant may release it, to prevent a rejected replayed connection from closing a normal session.
        return instance.primaryWebSockets.remove(token, webSocketId);
    }


    private static String createToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }


}

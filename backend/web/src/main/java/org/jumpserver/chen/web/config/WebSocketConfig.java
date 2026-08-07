package org.jumpserver.chen.web.config;

import lombok.extern.slf4j.Slf4j;
import org.jumpserver.chen.framework.session.SessionManager;
import org.jumpserver.chen.framework.ws.ConsoleWebSocketHandler;
import org.jumpserver.chen.framework.ws.DBConsoleWebsocketHandler;
import org.jumpserver.chen.framework.ws.SessionWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class WebSocketConfig {


    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(1024 * 1024);
        // Optional: async send timeout
        // container.setAsyncSendTimeout(20_000L);
        return container;
    }


    @Bean
    public WebSocketHandlerMapping chenWebSocketHandlerMapping() {
        var handlers = new LinkedHashMap<String, Object>();
        handlers.put("/ws/session", createRequestHandler(new SessionWebSocketHandler()));
        handlers.put("/ws/console", createRequestHandler(new ConsoleWebSocketHandler()));
        handlers.put("/ws/db-console", createRequestHandler(new DBConsoleWebsocketHandler()));

        var mapping = new WebSocketHandlerMapping();
        // Matches Spring's default WebSocket mapping, ensuring WS requests are handled before regular MVC mappings.
        mapping.setOrder(1);
        mapping.setUrlMap(handlers);
        return mapping;
    }

    private WebSocketHttpRequestHandler createRequestHandler(WebSocketHandler webSocketHandler) {
        var requestHandler = new WebSocketHttpRequestHandler(webSocketHandler);
        // Only use Chen's dynamic validation, to avoid WebSocketHandlerRegistry appending a second Origin interceptor.
        requestHandler.setHandshakeInterceptors(List.of(new ServletWebSocketHandshakeInterceptor()));
        return requestHandler;
    }

    // Exact allowlist used only for cross-host requests, formatted as a comma-separated list of host or host:port.
    private static final Set<String> TRUSTED_DOMAINS =
            Arrays.stream(
                            Optional.ofNullable(System.getenv("DOMAINS"))
                                    .orElse("")
                                    .split(",")
                    )
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());


    static boolean checkOrigin(String origin, InetSocketAddress requestHost, Set<String> trustedDomains) {
        if (origin == null || origin.isBlank()) {
            return true;
        }

        if (trustedDomains.contains("*")) {
            return true;
        }

        try {
            URI uri = URI.create(origin);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }

            String originHost = normalizeHost(uri.getHost());
            if (originHost == null) {
                return false;
            }

            String normalizedRequestHost = requestHost == null
                    ? null
                    : normalizeHost(requestHost.getHostString());
            // Compare only the Host, to support the scenario where TLS is terminated at a proxy and forwarded to Chen's internal port.
            if (originHost.equals(normalizedRequestHost)) {
                return true;
            }

            // Allow localhost access through directly
            if (isLocalhost(originHost)) {
                return true;
            }

            return matchesTrustedDomains(uri, originHost, trustedDomains);

        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean matchesTrustedDomains(URI origin, String originHost, Set<String> trustedDomains) {
        // Cross-host requests must match DOMAINS exactly; suffix or substring matching is not allowed.
        if (containsIgnoreCase(trustedDomains, originHost)) {
            return true;
        }

        int port = origin.getPort();
        return port >= 0 && containsIgnoreCase(trustedDomains, formatHostAndPort(originHost, port));
    }

    private static boolean containsIgnoreCase(Set<String> domains, String value) {
        return domains.stream()
                .map(String::trim)
                .anyMatch(domain -> domain.equalsIgnoreCase(value));
    }

    private static String normalizeHost(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }

        String normalizedHost = host.trim();
        if (normalizedHost.startsWith("[") && normalizedHost.endsWith("]")) {
            normalizedHost = normalizedHost.substring(1, normalizedHost.length() - 1);
        }
        return normalizedHost.toLowerCase(Locale.ROOT);
    }

    private static boolean isLocalhost(String host) {
        return "localhost".equals(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host)
                || "0:0:0:0:0:0:0:1".equals(host);
    }

    private static String formatHostAndPort(String host, int port) {
        return host.indexOf(':') >= 0 ? "[" + host + "]:" + port : host + ":" + port;
    }

    public static class ServletWebSocketHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

            String origin = request.getHeaders().getOrigin();
            InetSocketAddress requestHost = request.getHeaders().getHost();
            // Some Servlet request objects don't populate the Host header; fall back to the host and port resolved by the container.
            if (requestHost == null && request instanceof ServletServerHttpRequest servletRequest) {
                requestHost = new InetSocketAddress(
                        servletRequest.getServletRequest().getServerName(),
                        servletRequest.getServletRequest().getServerPort());
            }

            if (!checkOrigin(origin, requestHost, TRUSTED_DOMAINS)) {
                log.warn("Reject WebSocket handshake: untrusted or invalid origin");
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            var protocols = request.getHeaders().get("Sec-WebSocket-Protocol");
            // Reject abnormal handshakes missing a subprotocol, to avoid an NPE from reading an empty list.
            if (protocols == null || protocols.isEmpty()) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            var token = protocols.get(0);
            var session = SessionManager.getSession(token);
            var servletRequest = request instanceof ServletServerHttpRequest servletRequestWrapper
                    ? servletRequestWrapper.getServletRequest() : null;
            var httpSession = servletRequest == null ? null : servletRequest.getSession(false);
            var sessionBound = session != null && httpSession != null && Objects.equals(
                    session.getAttribute(SessionManager.WEB_SESSION_ID_ATTRIBUTE), httpSession.getId());
            // The token must still be alive, and must come from the same browser HTTP session that created it.
            if (!sessionBound) {
                // Only log the validation dimensions; don't log sensitive info such as the token, cookie, or HTTP session ID.
                log.warn("Reject WebSocket handshake: tokenExists={}, httpSessionExists={}, sessionBound={}",
                        session != null, httpSession != null, sessionBound);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            log.info("Accept WebSocket handshake: HTTP session binding verified");
            attributes.put("token", token);
            response.getHeaders().put("Sec-WebSocket-Protocol", protocols);
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
            // After the handshake
        }
    }
}

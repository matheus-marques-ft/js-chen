package org.jumpserver.chen.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.jumpserver.chen.framework.session.SessionManager;


public class SessionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        //0. If this is a login request, allow it through directly
        if (!req.getServletPath().startsWith("/api") || req.getServletPath().equals("/api/auth")) {
            return true;
        }
        //1. Get the token from the header
        String token = req.getHeader("token");
        //2. Check whether the token is authenticated
        if (token == null || token.isEmpty() || SessionManager.getSession(token) == null || !SessionManager.getSession(token).isActive()) {
            //2.1 Authentication failed, return an error message
            resp.setStatus(401);
            resp.getWriter().write("Unauthorized");
            return false;
        }

        SessionManager.setContext(token);
        return true;
    }

}

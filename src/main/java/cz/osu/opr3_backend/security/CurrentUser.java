package cz.osu.opr3_backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

public final class CurrentUser {
    private CurrentUser() {}

    public static String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) return null;
        if (!auth.isAuthenticated()) return null;
        if (auth instanceof AnonymousAuthenticationToken) return null;
        if ("anonymousUser".equals(auth.getPrincipal())) return null;

        return auth.getName();
    }
}

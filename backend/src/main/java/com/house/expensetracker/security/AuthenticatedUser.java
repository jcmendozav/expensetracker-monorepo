package com.house.expensetracker.security;

import java.security.Principal;

/**
 * Immutable server-side security principal populated from a verified Firebase ID token.
 * Encapsulates caller identity (UID, email, display name) so controllers and services
 * never depend on client-supplied identity fields.
 */
public record AuthenticatedUser(String uid, String email, String displayName) implements Principal {

    @Override
    public String getName() {
        return uid;
    }

    /**
     * Safely resolves an {@link AuthenticatedUser} from any {@link Principal},
     * defining null/cast edge cases out of existence for both production filters and test slices.
     */
    public static AuthenticatedUser from(Principal principal) {
        if (principal == null) {
            return new AuthenticatedUser(null, null, null);
        }
        if (principal instanceof AuthenticatedUser authUser) {
            return authUser;
        }
        if (principal instanceof org.springframework.security.core.Authentication auth
                && auth.getPrincipal() instanceof AuthenticatedUser authUser) {
            return authUser;
        }
        return new AuthenticatedUser(principal.getName(), null, null);
    }
}

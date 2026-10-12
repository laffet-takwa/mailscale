package com.mailscale.common;

import com.mailscale.auth.Organization;
import com.mailscale.auth.User;
import java.util.Optional;

/**
 * Thread-local holder for the authenticated principal and the organization the
 * request is scoped to. Every request passes through the authentication filter
 * which sets this context; services read it to enforce tenant isolation.
 */
public final class OrganizationContext {

    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private OrganizationContext() {
    }

    public static void set(User user, Organization organization) {
        HOLDER.set(new Context(user, organization));
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static Optional<Context> current() {
        return Optional.ofNullable(HOLDER.get());
    }

    public static User requireUser() {
        return current().map(Context::user)
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }

    public static Organization requireOrganization() {
        return current().map(Context::organization)
                .orElseThrow(() -> new IllegalStateException("No organization in context"));
    }

    public record Context(User user, Organization organization) {
    }
}

package com.codingshuttle.linkedInProject.postsService.auth;

/**
 * Test-only bridge to the package-private {@link AuthContextHolder} setter, so
 * service/saga tests can pretend a request from a given user is in flight
 * without widening the production API.
 */
public final class AuthTestSupport {

    private AuthTestSupport() {
    }

    public static void setCurrentUser(Long userId) {
        AuthContextHolder.setCurrentUserId(userId);
    }

    public static void clear() {
        AuthContextHolder.clear();
    }
}

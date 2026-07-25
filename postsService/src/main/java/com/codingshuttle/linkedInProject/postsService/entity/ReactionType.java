package com.codingshuttle.linkedInProject.postsService.entity;

/**
 * The reactions a user can leave on a post. LIKE is first so it remains the
 * default - existing rows written before reactions existed are all LIKEs, and a
 * reaction request with no explicit type falls back to it.
 */
public enum ReactionType {
    LIKE,
    CELEBRATE,
    SUPPORT,
    INSIGHTFUL,
    FUNNY
}

package com.codingshuttle.linkedInProject.postsService.service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls mentioned user ids out of post/comment text. Mentions are stored as
 * {@code @[Display Name](userId)} tokens - the frontend inserts the id when the
 * author picks someone from the autocomplete, so resolution here is just parsing
 * the id rather than guessing a user from a name. The display name may hold any
 * character except a closing bracket, and only a numeric id is accepted.
 */
final class MentionExtractor {

    private static final Pattern MENTION = Pattern.compile("@\\[[^\\]]*\\]\\((\\d+)\\)");

    private MentionExtractor() {
    }

    /** Distinct mentioned user ids, in first-seen order. Empty for null text. */
    static Set<Long> extract(String content) {
        Set<Long> ids = new LinkedHashSet<>();
        if (content == null) return ids;

        Matcher matcher = MENTION.matcher(content);
        while (matcher.find()) {
            ids.add(Long.valueOf(matcher.group(1)));
        }
        return ids;
    }
}

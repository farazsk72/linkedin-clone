package com.codingshuttle.linkedInProject.postsService.service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls #tags out of post text. Deliberately narrow: letters, digits and
 * underscore only, so "#tag." and "#tag," end at the punctuation, and a bare
 * "#" or a "#" inside a word (like "C#") does not produce a tag.
 */
final class HashtagExtractor {

    private static final Pattern HASHTAG = Pattern.compile("(?<![\\w#])#(\\w+)");

    private HashtagExtractor() {
    }

    static Set<String> extract(String content) {
        Set<String> tags = new LinkedHashSet<>();
        if (content == null) return tags;

        Matcher matcher = HASHTAG.matcher(content);
        while (matcher.find()) {
            tags.add(matcher.group(1).toLowerCase());
        }
        return tags;
    }
}

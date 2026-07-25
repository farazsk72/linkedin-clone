package com.codingshuttle.linkedInProject.postsService.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The extractor is the contract between the frontend's @[Name](id) token and the
 * mention notifications: it must read exactly the ids the author picked, dedupe
 * them, and never invent one from loose text.
 */
class MentionExtractorTest {

    @Test
    void extractsASingleMentionId() {
        assertThat(MentionExtractor.extract("hey @[Ada Lovelace](5) welcome"))
                .containsExactly(5L);
    }

    @Test
    void extractsMultipleIdsInFirstSeenOrder() {
        assertThat(MentionExtractor.extract("@[Grace](2) and @[Ada](5) and @[Alan](9)"))
                .containsExactly(2L, 5L, 9L);
    }

    @Test
    void dedupesRepeatedMentionsOfTheSameUser() {
        assertThat(MentionExtractor.extract("@[Ada](5) ... @[Ada Lovelace](5) again"))
                .containsExactly(5L);
    }

    @Test
    void handlesNamesContainingSpacesAndPunctuation() {
        assertThat(MentionExtractor.extract("@[Dr. Grace M. Hopper, PhD](2)"))
                .containsExactly(2L);
    }

    @Test
    void ignoresTextThatIsNotACompleteToken() {
        // A bare @name, a token missing its id, and a non-numeric id are all
        // left alone - only the frontend's real token counts.
        assertThat(MentionExtractor.extract("@ada @[Ada] @[Ada](abc) #tag plain text"))
                .isEmpty();
    }

    @Test
    void returnsEmptyForNullOrPlainContent() {
        assertThat(MentionExtractor.extract(null)).isEmpty();
        assertThat(MentionExtractor.extract("just some ordinary text")).isEmpty();
    }

    @Test
    void preservesInsertionOrderAsAList() {
        // Ordering matters for deterministic notifications, so assert it as a list.
        assertThat(List.copyOf(MentionExtractor.extract("@[B](20) @[A](10)")))
                .containsExactly(20L, 10L);
    }
}

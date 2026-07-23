package com.codingshuttle.linkedInProject.postsService.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests - no Spring, no mocks. Pins down the hashtag regex edge cases
 * that are easy to get subtly wrong.
 */
class HashtagExtractorTest {

    @Test
    @DisplayName("extracts tags, lower-cases them, and de-duplicates")
    void extractsLowercasesAndDedupes() {
        assertThat(HashtagExtractor.extract("Learning #Java and #java and #Spring"))
                .containsExactlyInAnyOrder("java", "spring");
    }

    @Test
    @DisplayName("stops a tag at trailing punctuation")
    void stopsAtPunctuation() {
        assertThat(HashtagExtractor.extract("great #spring. and #kafka!"))
                .containsExactlyInAnyOrder("spring", "kafka");
    }

    @Test
    @DisplayName("does not treat '#' inside a word (e.g. C#) as a tag")
    void ignoresHashInsideWord() {
        assertThat(HashtagExtractor.extract("I know C# and F#")).isEmpty();
    }

    @Test
    @DisplayName("a bare '#' produces no tag")
    void bareHashProducesNothing() {
        assertThat(HashtagExtractor.extract("just a # symbol")).isEmpty();
    }

    @Test
    @DisplayName("null or empty content yields an empty set")
    void nullSafe() {
        assertThat(HashtagExtractor.extract(null)).isEmpty();
        assertThat(HashtagExtractor.extract("")).isEmpty();
    }

    @Test
    @DisplayName("preserves order of first appearance")
    void preservesInsertionOrder() {
        Set<String> tags = HashtagExtractor.extract("#zebra then #alpha then #zebra");
        assertThat(tags).containsExactly("zebra", "alpha");
    }
}

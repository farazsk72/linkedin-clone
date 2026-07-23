package com.codingshuttle.linkedInProject.postsService.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * A stable wire shape for paged results. Spring's own Page implementation is
 * explicitly documented as unstable to serialise, so the API exposes this
 * instead of leaking PageImpl's JSON.
 */
@Data
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public static <E, T> PageResponse<T> from(Page<E> source, Function<E, T> mapper) {
        PageResponse<T> response = new PageResponse<>();
        response.setContent(source.getContent().stream().map(mapper).toList());
        copyMetadata(response, source);
        return response;
    }

    /**
     * For callers that build the whole content list in one batch (avoiding the
     * per-element N+1 that {@link #from} invites) and just need the page
     * metadata copied across.
     */
    public static <E, T> PageResponse<T> fromContent(Page<E> source, List<T> content) {
        PageResponse<T> response = new PageResponse<>();
        response.setContent(content);
        copyMetadata(response, source);
        return response;
    }

    private static void copyMetadata(PageResponse<?> response, Page<?> source) {
        response.setPage(source.getNumber());
        response.setSize(source.getSize());
        response.setTotalElements(source.getTotalElements());
        response.setTotalPages(source.getTotalPages());
        response.setLast(source.isLast());
    }
}

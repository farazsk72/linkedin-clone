package com.codingshuttle.linkedInProject.userService.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * A stable wire shape for paged results, matching the one postsService exposes.
 * Spring's own Page implementation is documented as unstable to serialise, so
 * the API returns this instead of leaking PageImpl's JSON.
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
        response.setPage(source.getNumber());
        response.setSize(source.getSize());
        response.setTotalElements(source.getTotalElements());
        response.setTotalPages(source.getTotalPages());
        response.setLast(source.isLast());
        return response;
    }
}

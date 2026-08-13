package com.srm.analytics.dto;

import java.util.List;
import org.springframework.data.domain.Pageable;

/** Resposta paginada (server-side) com metadados. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty) {

    public static <T> PageResponse<T> of(List<T> content, Pageable pageable, long totalElements) {
        int size =
                pageable.getPageSize() == 0 ? Math.max(1, content.size()) : pageable.getPageSize();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int pageNumber = pageable.getPageNumber();
        return new PageResponse<>(
                content,
                pageNumber,
                size,
                totalElements,
                totalPages,
                pageNumber == 0,
                pageNumber >= totalPages - 1,
                content.isEmpty());
    }
}

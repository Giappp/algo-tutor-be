package org.rap.algotutorbe.common.api;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Builder
@Data
public class PageResponse<T> {
    List<T> data;
    Integer pageSize;
    Integer totalPages;
    Long totalElements;
    Integer currentPage;

    public static <T> PageResponse<T> empty() {
        return PageResponse.<T>builder()
                .data(List.of())
                .pageSize(0)
                .totalPages(0)
                .totalElements(0L)
                .currentPage(1)
                .build();
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .data(page.getContent())
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .currentPage(page.getNumber())
                .build();
    }
}
package org.rap.algotutorbe.common.api;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class PageResponse<T> {
    List<T> data;
    Integer pageSize;
    Integer totalPages;
    Long totalElements;
    Integer currentPage;
}
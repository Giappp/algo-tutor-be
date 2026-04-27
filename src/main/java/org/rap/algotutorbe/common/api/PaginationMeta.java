package org.rap.algotutorbe.common.api;

public record PaginationMeta(
        int page,
        int limit,
        long total,
        int totalPages
) {
}


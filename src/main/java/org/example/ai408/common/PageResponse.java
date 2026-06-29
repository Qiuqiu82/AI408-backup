package org.example.ai408.common;

import java.util.List;

public record PageResponse<T>(
        int pageIndex,
        int pageSize,
        int pageCount,
        long recordCount,
        List<T> records
) {
}

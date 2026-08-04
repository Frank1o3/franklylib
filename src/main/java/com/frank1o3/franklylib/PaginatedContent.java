package com.frank1o3.franklylib;

import java.util.List;

public record PaginatedContent<T>(List<T> items, int page, int pageSize, int totalPages, boolean hasPrevious,
        boolean hasNext) {
    public static <T> PaginatedContent<T> of(List<T> items, int page, int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be > 0");
        }
        int safePage = Math.max(0, page);
        int totalPages = items.isEmpty() ? 1 : (int) Math.ceil((double) items.size() / pageSize);
        int clampedPage = Math.min(safePage, Math.max(0, totalPages - 1));
        int start = clampedPage * pageSize;
        int end = Math.min(start + pageSize, items.size());
        List<T> visible = items.subList(start, end);
        return new PaginatedContent<>(visible, clampedPage, pageSize, totalPages, clampedPage > 0,
                clampedPage + 1 < totalPages);
    }
}

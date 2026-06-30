package com.api.bedhcd.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> items;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;

    public static <T> PageResponse<T> of(List<T> items, long totalElements, int page, int size) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return PageResponse.<T>builder()
                .items(items)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .currentPage(page)
                .size(size)
                .build();
    }
}

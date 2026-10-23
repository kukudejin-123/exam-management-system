package com.exam.api.dto;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页响应体。
 *
 * <p>与 common 层的 PageResult 分开：对外暴露的字段更少更稳定，
 * 内部重构不会直接影响前端。
 */
public record PageResponse<T>(List<T> records, long total, long pageNo, long pageSize, long pages) {

    public static <S, T> PageResponse<T> from(com.exam.common.page.PageResult<S> page,
                                              Function<S, T> mapper) {
        List<T> mapped = page.getRecords().stream().map(mapper).collect(Collectors.toList());
        return new PageResponse<>(mapped, page.getTotal(), page.getPageNo(),
                page.getPageSize(), page.getPages());
    }
}

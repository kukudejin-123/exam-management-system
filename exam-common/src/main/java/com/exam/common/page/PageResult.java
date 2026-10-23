package com.exam.common.page;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** 分页结果体 */
public final class PageResult<T> {

    private final List<T> records;
    private final long total;
    private final long pageNo;
    private final long pageSize;

    private PageResult(List<T> records, long total, long pageNo, long pageSize) {
        this.records = records == null ? Collections.emptyList() : records;
        this.total = total;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }

    public static <T> PageResult<T> of(List<T> records, long total, long pageNo, long pageSize) {
        return new PageResult<>(records, total, pageNo, pageSize);
    }

    public static <T> PageResult<T> empty(long pageNo, long pageSize) {
        return new PageResult<>(Collections.emptyList(), 0L, pageNo, pageSize);
    }

    public List<T> getRecords() {
        return records;
    }

    public long getTotal() {
        return total;
    }

    public long getPageNo() {
        return pageNo;
    }

    public long getPageSize() {
        return pageSize;
    }

    /** 总页数。总条数为 0 时返回 0 页；每页条数为 0 按 1 算，避免除零 */
    public long getPages() {
        if (total <= 0) {
            return 0;
        }
        long size = pageSize <= 0 ? 1 : pageSize;
        return (total + size - 1) / size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageResult<?> other)) {
            return false;
        }
        return total == other.total && pageNo == other.pageNo && pageSize == other.pageSize
                && Objects.equals(records, other.records);
    }

    @Override
    public int hashCode() {
        return Objects.hash(records, total, pageNo, pageSize);
    }

    @Override
    public String toString() {
        return "PageResult{total=" + total + ", pageNo=" + pageNo + ", pageSize=" + pageSize + '}';
    }
}

package com.exam.common.page;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分页查询入参。默认第 1 页、每页 10 条，最大 100 条，防止误传超大 pageSize 拖垮数据库。
 */
public class PageQuery {

    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    @Min(value = 1, message = "页码不能小于 1")
    private long pageNo = DEFAULT_PAGE_NO;

    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = MAX_PAGE_SIZE, message = "每页条数不能超过 100")
    private long pageSize = DEFAULT_PAGE_SIZE;

    public long getPageNo() {
        return pageNo;
    }

    public void setPageNo(long pageNo) {
        this.pageNo = pageNo;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    /** MyBatis-Plus 的偏移量 */
    public long offset() {
        return (Math.max(pageNo, DEFAULT_PAGE_NO) - 1) * effectivePageSize();
    }

    /** 归一化后的每页条数 */
    public long effectivePageSize() {
        if (pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}

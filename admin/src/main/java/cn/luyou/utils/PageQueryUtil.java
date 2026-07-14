package cn.luyou.utils;

/**
 * 分页查询参数校验，防止单次请求拉取过多数据导致接口与前端卡顿。
 */
public final class PageQueryUtil {

    public static final int MAX_PAGE_SIZE = 1000;

    private PageQueryUtil() {
    }

    public static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}

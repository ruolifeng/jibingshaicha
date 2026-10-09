package cn.luyou.utils;

/**
 * 基于ThreadLocal封装工具类，用于保存和获取当前登录用户的id、角色、所属部门
 */
public class BaseContext {

    private static final ThreadLocal<Long>    userIdLocal       = new ThreadLocal<>();
    private static final ThreadLocal<Integer> roleLocal         = new ThreadLocal<>();
    private static final ThreadLocal<Long>    departmentIdLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        userIdLocal.set(id);
    }

    public static Long getCurrentId() {
        return userIdLocal.get();
    }

    public static void setCurrentRole(Integer role) {
        roleLocal.set(role);
    }

    public static Integer getCurrentRole() {
        return roleLocal.get();
    }

    public static void setCurrentDepartmentId(Long departmentId) {
        departmentIdLocal.set(departmentId);
    }

    public static Long getCurrentDepartmentId() {
        return departmentIdLocal.get();
    }

    /** 判断当前用户是否为超级管理员（role=1） */
    public static boolean isSuperAdmin() {
        return Integer.valueOf(1).equals(roleLocal.get());
    }

    /**
     * 三级及以上：超管 / 一级 / 二级 / 三级（role 1–4）。
     * 四级、五级不可使用跨区转出源记录的查阅与随访修改。
     */
    public static boolean isLevel3OrAbove() {
        Integer role = roleLocal.get();
        return role != null && role >= 1 && role <= 4;
    }

    public static void remove() {
        userIdLocal.remove();
        roleLocal.remove();
        departmentIdLocal.remove();
    }
}
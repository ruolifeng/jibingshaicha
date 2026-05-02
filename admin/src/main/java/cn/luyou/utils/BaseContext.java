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

    public static void remove() {
        userIdLocal.remove();
        roleLocal.remove();
        departmentIdLocal.remove();
    }
}
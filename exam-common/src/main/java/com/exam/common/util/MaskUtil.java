package com.exam.common.util;

/** 脱敏工具：成绩册导出、阅卷匿名评阅时隐藏考生姓名。 */
public final class MaskUtil {

    private MaskUtil() {
    }

    /** 姓名脱敏：张小明 → 张*明；单字姓名 → * */
    public static String name(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        if (raw.length() == 1) {
            return "*";
        }
        if (raw.length() == 2) {
            return raw.charAt(0) + "*";
        }
        StringBuilder sb = new StringBuilder(raw);
        for (int i = 1; i < raw.length() - 1; i++) {
            sb.setCharAt(i, '*');
        }
        return sb.toString();
    }

    /** 学号脱敏：保留前 3 位与后 2 位，如 2023010001 → 202*****01 */
    public static String userNo(String raw) {
        if (raw == null || raw.length() <= 5) {
            return raw;
        }
        int keepHead = 3;
        int keepTail = 2;
        StringBuilder sb = new StringBuilder(raw);
        for (int i = keepHead; i < raw.length() - keepTail; i++) {
            sb.setCharAt(i, '*');
        }
        return sb.toString();
    }
}

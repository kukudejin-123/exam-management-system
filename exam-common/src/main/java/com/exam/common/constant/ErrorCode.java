package com.exam.common.constant;

/**
 * 统一错误码。业务异常与系统异常分开段位，便于前端分流处理。
 *
 * <p>分段约定：1xxx 参数与校验 / 2xxx 认证授权 / 3xxx 资源不存在 / 4xxx 业务规则冲突 / 5xxx 系统内部。
 */
public final class ErrorCode {

    private ErrorCode() {
    }

    /** 参数不合法 */
    public static final int PARAM_INVALID = 1001;
    /** 重复提交 */
    public static final int DUPLICATE_SUBMIT = 1002;

    /** 未登录 / token 缺失或无效 */
    public static final int UNAUTHORIZED = 2001;
    /** 已登录但无权限 */
    public static final int FORBIDDEN = 2002;
    /** token 过期 */
    public static final int TOKEN_EXPIRED = 2003;
    /** 登录失败次数超限被锁 */
    public static final int LOGIN_LOCKED = 2004;
    /** 用户名或密码错误 */
    public static final int BAD_CREDENTIAL = 2005;

    /** 资源不存在 */
    public static final int NOT_FOUND = 3001;

    /** 状态冲突，如考试已开始后不允许再改卷 */
    public static final int STATE_CONFLICT = 4001;
    /** 组卷时候选题目不足：阻断并提示缺口数量，不允许静默降级 */
    public static final int INSUFFICIENT_QUESTIONS = 4002;
    /** 考试时间窗冲突 */
    public static final int EXAM_TIME_CONFLICT = 4003;
    /** 成绩取值规则与多次尝试的冲突 */
    public static final int SCORE_RULE_CONFLICT = 4004;

    /** 系统内部错误 */
    public static final int INTERNAL_ERROR = 5001;
}

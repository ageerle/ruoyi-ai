package org.ruoyi.workflow.cosntant;

public class RedisKeyConstant {

    /**
     * 账号激活码的key
     */
    public static final String AUTH_ACTIVE_CODE = "auth:activeCode:{0}";
    /**
     * 注册时使用的验证码
     * 参数：验证码id
     * 值：验证码
     */
    public static final String AUTH_REGISTER_CAPTCHA_ID = "auth:register:captcha:{0}";
    /**
     * 登录时使用的验证码id缓存
     * 参数：验证码id
     * 值：验证码
     */
    public static final String AUTH_LOGIN_CAPTCHA_ID = "auth:login:captcha:{0}";
    /**
     * 注册验证码缓存
     * 参数：验证码
     * 值：1
     */
    public static final String AUTH_CAPTCHA = "auth:register:captcha:{0}";
    /**
     * 登录token
     * {0}:用户token
     * 值：json.format(user)
     */
    public static final String USER_TOKEN = "user:token:{0}";
    /**
     * 参数：游客的uuid
     * 值：json.format(guest)
     */
    public static final String GUEST_UUID = "guest:uuid:{0}";
    /**
     * 登录失败次数
     * 参数：用户邮箱
     * 值: 失效次数
     */
    public static final String LOGIN_FAIL_COUNT = "user:login:fail:{0}";
    /**
     * 用户是否请求ai中
     * 参数：用户id
     * 值: 1或者0
     */
    public static final String USER_ASKING = "user:asking:{0}";
    /**
     * 用户是否画画中
     * 参数：用户id
     * 值: 1或者0
     */
    public static final String USER_DRAWING = "user:drawing:{0}";
    /**
     * 用户提问限流计数
     * 参数：用户id
     * 值: 当前时间窗口访问量
     */
    public static final String USER_REQUEST_TEXT_TIMES = "user:request-text:times:{0}";
    public static final String USER_REQUEST_IMAGE_TIMES = "user:request-image:times:{0}";
    /**
     * 用户信息缓存
     * 参数：用户id
     * 值: user object
     */
    public static final String USER_INFO = "user:info:";
    /**
     * 找回密码的请求绑在
     * 参数：随机数
     * 值: 用户id，用于校验后续流程中的重置密码使用
     */
    public static final String FIND_MY_PASSWORD = "user:find:password:{0}";
    /**
     * qa提问次数（每天）
     * 参数：用户id:日期yyyyMMdd
     * 值：提问数量
     */
    public static final String AQ_ASK_TIMES = "qa:ask:limit:{0}:{1}";
    /**
     * 知识库知识点生成数量
     * 值: 用户id
     */
    public static final String QA_ITEM_CREATE_LIMIT = "aq:item:create:{0}";
    /**
     * 信号(重新生成知识库统计数据)
     * 值:知识库uuid
     */
    public static final String KB_STATISTIC_RECALCULATE_SIGNAL = "kb:statistic:recalculate:signal";
    public static final String STATISTIC = "statistic";
    public static final String STATISTIC_USER = "user";
    public static final String STATISTIC_KNOWLEDGE_BASE = "kb";
    public static final String STATISTIC_TOKEN_COST = "token-cost";
    public static final String STATISTIC_CONVERSATION = "conversation";
    public static final String STATISTIC_IMAGE_COST = "image-cost";
    public static final String TOKEN_USAGE_KEY = "token:usage:{0}";
    /**
     * 用户正在对文档进行索引
     * 值：用户id
     */
    public static final String USER_INDEXING = "user:indexing:{0}";
    /**
     * 用户评论并发限制
     * 值：用户id
     */
    public static final String DRAW_COMMENT_LIMIT_KEY = "user:draw:comment-submitting:{0}";
    public static final String WORKFLOW_KEY = "workflow";
    public static final String WORKFLOW_COMPONENTS = "workflow:components";
    public static final String WORKFLOW_COMPONENT_START_KEY = "workflow:component:start";
    public static final String WORKFLOW_COMPONENT_KEY = "workflow:component";
    public static final String WORKFLOW_COPY_DOING = "workflow:copy:doing:{0}";

    private RedisKeyConstant() {
    }
}

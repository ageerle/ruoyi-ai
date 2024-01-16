package com.xmzs.common.core.constant;

/**
 * 返回状态码
 *
 * @author Lion Li
 */
public interface HttpStatus {
    /**
     * 操作成功
     */
    int SUCCESS = 200;

    /**
     * 对象创建成功
     */
    int CREATED = 201;

    /**
     * 请求已经被接受
     */
    int ACCEPTED = 202;

    /**
     * 操作已经执行成功，但是没有返回数据
     */
    int NO_CONTENT = 204;

    /**
     * 资源已被移除
     */
    int MOVED_PERM = 301;

    /**
     * 重定向
     */
    int SEE_OTHER = 303;

    /**
     * 资源没有被修改
     */
    int NOT_MODIFIED = 304;

    /**
     * 参数列表错误（缺少，格式不匹配）
     */
    int BAD_REQUEST = 400;

    /**
     * 未授权
     */
    int UNAUTHORIZED = 401;

    /**
     * 访问受限，授权过期
     */
    int FORBIDDEN = 403;

    /**
     * 资源，服务未找到
     */
    int NOT_FOUND = 404;

    /**
     * 不允许的http方法
     */
    int BAD_METHOD = 405;

    /**
     * 资源冲突，或者资源被锁
     */
    int CONFLICT = 409;

    /**
     * 不支持的数据，媒体类型
     */
    int UNSUPPORTED_TYPE = 415;

    /**
     * 系统内部错误
     */
    int ERROR = 500;

    /**
     * 接口未实现
     */
    int NOT_IMPLEMENTED = 501;

    /**
     * 系统警告消息
     */
    int WARN = 601;
}

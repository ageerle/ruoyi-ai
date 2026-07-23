package org.ruoyi.common.trace.constant;

/**
 * 通用链路追踪常量。
 */
public final class TraceConstants {

    private TraceConstants() {
    }

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String NODE_METHOD = "METHOD";
    public static final String NODE_HTTP = "HTTP";
    public static final String NODE_DB = "DB";
    public static final String NODE_CACHE = "CACHE";
    public static final String NODE_TASK = "TASK";
    public static final String NODE_STREAM = "STREAM";
}

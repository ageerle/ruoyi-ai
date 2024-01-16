package com.xmzs.common.log.aspect;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.xmzs.common.core.utils.ServletUtils;
import com.xmzs.common.core.utils.SpringUtils;
import com.xmzs.common.core.utils.StringUtils;
import com.xmzs.common.json.utils.JsonUtils;
import com.xmzs.common.log.annotation.Log;
import com.xmzs.common.log.enums.BusinessStatus;
import com.xmzs.common.log.event.OperLogEvent;
import com.xmzs.common.satoken.utils.LoginHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.Map;

/**
 * 操作日志记录处理
 *
 * @author Lion Li
 */
@Slf4j
@Aspect
@AutoConfiguration
public class LogAspect {

    /**
     * 排除敏感属性字段
     */
    public static final String[] EXCLUDE_PROPERTIES = { "password", "oldPassword", "newPassword", "confirmPassword" };


    /**
     * 计算操作消耗时间
     */
    private static final ThreadLocal<StopWatch> TIME_THREADLOCAL = new TransmittableThreadLocal<>();

    /**
     * 处理请求前执行
     */
    @Before(value = "@annotation(controllerLog)")
    public void boBefore(JoinPoint joinPoint, Log controllerLog) {
        StopWatch stopWatch = new StopWatch();
        TIME_THREADLOCAL.set(stopWatch);
        stopWatch.start();
    }

    /**
     * 处理完请求后执行
     *
     * @param joinPoint 切点
     */
    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult) {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    /**
     * 拦截异常操作
     *
     * @param joinPoint 切点
     * @param e         异常
     */
    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e) {
        handleLog(joinPoint, controllerLog, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception e, Object jsonResult) {
        try {

            // *========数据库日志=========*//
            OperLogEvent operLog = new OperLogEvent();
            operLog.setTenantId(LoginHelper.getTenantId());
            operLog.setStatus(BusinessStatus.SUCCESS.ordinal());
            // 请求的地址
            String ip = ServletUtils.getClientIP();
            operLog.setOperIp(ip);
            operLog.setOperUrl(StringUtils.substring(ServletUtils.getRequest().getRequestURI(), 0, 255));
            operLog.setOperName(LoginHelper.getUsername());

            if (e != null) {
                operLog.setStatus(BusinessStatus.FAIL.ordinal());
                operLog.setErrorMsg(StringUtils.substring(e.getMessage(), 0, 2000));
            }
            // 设置方法名称
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");
            // 设置请求方式
            operLog.setRequestMethod(ServletUtils.getRequest().getMethod());
            // 处理设置注解上的参数
            getControllerMethodDescription(joinPoint, controllerLog, operLog, jsonResult);
            // 设置消耗时间
            StopWatch stopWatch = TIME_THREADLOCAL.get();
            stopWatch.stop();
            operLog.setCostTime(stopWatch.getTime());
            // 发布事件保存数据库
            SpringUtils.context().publishEvent(operLog);
        } catch (Exception exp) {
            // 记录本地异常日志
            log.error("异常信息:{}", exp.getMessage());
            exp.printStackTrace();
        } finally {
            TIME_THREADLOCAL.remove();
        }
    }

    /**
     * 获取注解中对方法的描述信息 用于Controller层注解
     *
     * @param log     日志
     * @param operLog 操作日志
     * @throws Exception
     */
    public void getControllerMethodDescription(JoinPoint joinPoint, Log log, OperLogEvent operLog, Object jsonResult) throws Exception {
        // 设置action动作
        operLog.setBusinessType(log.businessType().ordinal());
        // 设置标题
        operLog.setTitle(log.title());
        // 设置操作人类别
        operLog.setOperatorType(log.operatorType().ordinal());
        // 是否需要保存request，参数和值
        if (log.isSaveRequestData()) {
            // 获取参数的信息，传入到数据库中。
            setRequestValue(joinPoint, operLog, log.excludeParamNames());
        }
        // 是否需要保存response，参数和值
        if (log.isSaveResponseData() && ObjectUtil.isNotNull(jsonResult)) {
            operLog.setJsonResult(StringUtils.substring(JsonUtils.toJsonString(jsonResult), 0, 2000));
        }
    }

    /**
     * 获取请求的参数，放到log中
     *
     * @param operLog 操作日志
     * @throws Exception 异常
     */
    private void setRequestValue(JoinPoint joinPoint, OperLogEvent operLog, String[] excludeParamNames) throws Exception {
        Map<String, String> paramsMap = ServletUtils.getParamMap(ServletUtils.getRequest());
        String requestMethod = operLog.getRequestMethod();
        if (MapUtil.isEmpty(paramsMap)
                && HttpMethod.PUT.name().equals(requestMethod) || HttpMethod.POST.name().equals(requestMethod)) {
            String params = argsArrayToString(joinPoint.getArgs(), excludeParamNames);
            operLog.setOperParam(StringUtils.substring(params, 0, 2000));
        } else {
            MapUtil.removeAny(paramsMap, EXCLUDE_PROPERTIES);
            MapUtil.removeAny(paramsMap, excludeParamNames);
            operLog.setOperParam(StringUtils.substring(JsonUtils.toJsonString(paramsMap), 0, 2000));
        }
    }

    /**
     * 参数拼装
     */
    private String argsArrayToString(Object[] paramsArray, String[] excludeParamNames) {
        StringBuilder params = new StringBuilder();
        if (paramsArray != null && paramsArray.length > 0) {
            for (Object o : paramsArray) {
                if (ObjectUtil.isNotNull(o) && !isFilterObject(o)) {
                    try {
                        String str = JsonUtils.toJsonString(o);
                        Dict dict = JsonUtils.parseMap(str);
                        if (MapUtil.isNotEmpty(dict)) {
                            MapUtil.removeAny(dict, EXCLUDE_PROPERTIES);
                            MapUtil.removeAny(dict, excludeParamNames);
                            str = JsonUtils.toJsonString(dict);
                        }
                        params.append(str).append(" ");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return params.toString().trim();
    }

    /**
     * 判断是否需要过滤的对象。
     *
     * @param o 对象信息。
     * @return 如果是需要过滤的对象，则返回true；否则返回false。
     */
    @SuppressWarnings("rawtypes")
    public boolean isFilterObject(final Object o) {
        Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection collection = (Collection) o;
            for (Object value : collection) {
                return value instanceof MultipartFile;
            }
        } else if (Map.class.isAssignableFrom(clazz)) {
            Map map = (Map) o;
            for (Object value : map.entrySet()) {
                Map.Entry entry = (Map.Entry) value;
                return entry.getValue() instanceof MultipartFile;
            }
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse
            || o instanceof BindingResult;
    }
}

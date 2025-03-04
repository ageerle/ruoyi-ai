package org.ruoyi.common.wechat.web.interceptor;

import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;
import org.ruoyi.common.wechat.web.utils.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 对外接口访问日志
 * @author WesleyOne
 * @create 2018/9/25
 */
public class VisitLogInterceptor4down implements Interceptor {

    public static final Logger LOG = LoggerFactory.getLogger(VisitLogInterceptor4down.class);

    @Override
    public void intercept(Invocation inv) {
        String ip = IpUtil.getRealIp(inv.getController().getRequest());
        StringBuffer requestURL = inv.getController().getRequest().getRequestURL();
        String queryString = inv.getController().getRequest().getQueryString();
        LOG.info("{} 操作了 {} {}",ip,requestURL,queryString);
        inv.invoke();
    }
}

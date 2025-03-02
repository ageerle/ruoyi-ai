package org.ruoyi.common.wechat.web.interceptor;

import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;
import org.ruoyi.common.wechat.web.base.BaseError;
import org.ruoyi.common.wechat.web.base.BaseException;
import org.ruoyi.common.wechat.web.base.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 全局异常拦截
 * @author WesleyOne
 * @create 2018/7/28
 */
public class ExceptionInterceptor implements Interceptor {

    private static final Logger LOG	= LoggerFactory.getLogger(ExceptionInterceptor.class);

    @Override
    public void intercept(Invocation me) {
        try {
            me.getController().setAttr("code","00");
            me.getController().setAttr("message","操作成功");
            me.invoke();
        }
        catch (Exception e) {
            LOG.error(e.getMessage(),e);
            BaseResponse resp = new BaseResponse();
            Throwable cause = e.getCause();
            String ajax = me.getController().getRequest().getHeader("X-Requested-With");
            //判断ajax请求还是页面请求
            if ("XMLHttpRequest".equals(ajax)){
                if (cause instanceof BaseException) {
                    resp.setCode(((BaseException) cause).getCode());
                    resp.setMessage(cause.getMessage());
                } else{
                    resp.setCode(BaseError.SYSTEM_ERR.getCode());
                    resp.setMessage(BaseError.SYSTEM_ERR.getMsg());
                }
                me.getController().renderJson(resp);
                return;
            }else{
                //默认系统500页面，添加第二个参数可自行添加500页面
                me.getController().renderError(500);
                return;
            }

        }
    }
}

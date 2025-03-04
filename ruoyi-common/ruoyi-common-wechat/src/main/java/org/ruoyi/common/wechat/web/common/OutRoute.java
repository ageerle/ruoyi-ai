package org.ruoyi.common.wechat.web.common;

import com.jfinal.config.Routes;
import org.ruoyi.common.wechat.web.controller.ExtendController;
import org.ruoyi.common.wechat.web.interceptor.VisitLogInterceptor4down;

/**
 * 对外路由统一管理
 * @author WesleyOne
 * @create 2018/9/25
 */
public class OutRoute extends Routes {
    @Override
    public void config() {
        //设置视图根目录
        setBaseViewPath("/WEB-INF/templates");
        addInterceptor(new VisitLogInterceptor4down());
        //添加路由
        add("/ext", ExtendController.class);
    }
}

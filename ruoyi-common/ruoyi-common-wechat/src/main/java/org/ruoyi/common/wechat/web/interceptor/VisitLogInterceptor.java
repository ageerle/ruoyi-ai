package org.ruoyi.common.wechat.web.interceptor;


import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;
import org.ruoyi.common.wechat.web.annotation.UnCheckLogin;
import org.ruoyi.common.wechat.web.cache.UserSession;
import org.ruoyi.common.wechat.web.utils.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;


/**
 * 管理后台操作日志
 * @author admin
 */
public class VisitLogInterceptor implements Interceptor {
	public static final Logger LOG = LoggerFactory.getLogger(VisitLogInterceptor.class);

	@Override
	public void intercept(Invocation inv) {
		String requestUrl = inv.getActionKey();
		String uid = inv.getController().getCookie("uid");
		String sid = inv.getController().getCookie("sid");
		String ip = IpUtil.getRealIp(inv.getController().getRequest());
        LOG.info("{} - {} - {} 操作了 {}",ip,uid,sid,requestUrl);

		inv.getController().setAttr("active",inv.getController().getControllerKey());

		//找到不需要登录的action
		Class controllerClass = inv.getController().getClass();
		UnCheckLogin methodOwn = getControllerMethodUnLoginOwn(controllerClass, inv.getMethodName());
		if (methodOwn != null) {
			LOG.info("不需要登录,requestUrl=" + requestUrl);
			inv.invoke();
			return;
		}

		boolean isLogin = UserSession.checkUserSession(uid,sid);
		if (!isLogin){
			//未登入
			inv.getController().redirect("/login",false);
			return;
		}

		long start = System.currentTimeMillis();
		inv.invoke();
		long l = System.currentTimeMillis() - start;
		if (l > 1000*2){
			LOG.warn("请求 {} ,连接时长 {} ms",requestUrl,l);
		}
	}

	//--------------以下是内部方法-----------

	private UnCheckLogin getControllerMethodUnLoginOwn(Class controllerClass, String methodName) {
		for (Method method : controllerClass.getMethods()) {
			if (methodName.equals(method.getName())) {
				return getUnLogin(method);
			}
		}
		return null;
	}

	private UnCheckLogin getUnLogin(Method method) {
		Annotation[] annotations = method.getAnnotations();
		for (Annotation annt : annotations) {
			if (annt instanceof UnCheckLogin) {
				UnCheckLogin own = (UnCheckLogin) annt;
				return own;
			}
		}
		return null;
	}

}

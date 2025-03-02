package org.ruoyi.common.wechat.web.controller;

import com.jfinal.kit.PropKit;
import org.apache.commons.lang3.RandomStringUtils;
import org.ruoyi.common.wechat.web.annotation.UnCheckLogin;
import org.ruoyi.common.wechat.web.cache.UserSession;

import java.util.UUID;

/**
 * @author WesleyOne
 * @create 2018/7/27
 */
public class IndexController extends _BaseController {

    public static final String WX_ROB_LOGIN_ = "WX_ROB_LOGIN_";

    public void index(){

        setAttr("username",getCookie("uid"));
        setAttr("imgdomain",PropKit.get("imgDomain"));


        renderTemplate("index.html");
    }

    @UnCheckLogin
    public void login(){
        redirect("/",false);
        // 测试免登录
        if (PropKit.getBoolean("devMode")){
            redirect("/loginPost?username=wxwobot&password=wxwobot",true);
            return;
        }
        renderTemplate("login.html");
    }

    /**
     * 登入
     */
    @UnCheckLogin
    public void loginPost(){
        String sid = UUID.randomUUID().toString();
        UserSession.addUserSession("wxwobot",sid);
        addCookie("uid","wxwobot",-1);
        addCookie("sid",sid,-1);
        redirect("/",false);
        //return;


//        String username = getPara("username");
//        String password = getPara("password");
//        if (vldParamNull(username,"用户名不能为空")||
//                vldParamNull(password,"密码不能为空")){
//            return;
//        }
//
//        // 获取真实密码。此处为了方便直接从配置文件里获取
//        String pass = PropKit.use("passport.properties").get(username);
//        // 清楚缓存,可以修改配置文件直接修改
//        PropKit.useless("passport.properties");
//
//        if (StringUtils.isNotEmpty(password) && pass != null && pass.equals(MD5Util.MD5Encrypt(password))){
//
//            //String sid = UUID.randomUUID().toString();
//            UserSession.addUserSession(username,sid);
//            addCookie("uid",username,-1);
//            addCookie("sid",sid,-1);
//            redirect("/",false);
//            return;
//        }else {
//            setAttr("error","账号密码不正确");
//            renderTemplate("login.html");
//        }
    }

    @UnCheckLogin
    public void logout(){
        String uid = getUid();
        if (uid!=null){
            UserSession.delUserSession(uid);
        }
        redirect("/login",false);
    }

    /**
     * 游客登录
     */
    @UnCheckLogin
    public void visitLogin() {
        String visitName = RandomStringUtils.randomAlphabetic(5);
        String sid = UUID.randomUUID().toString();
        UserSession.addUserSession(visitName,sid);
        addCookie("uid",visitName,-1);
        addCookie("sid",sid,-1);
        redirect("/",false);
    }

}

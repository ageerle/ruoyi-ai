package org.ruoyi.common.wechat.web.controller;

import com.alibaba.fastjson.JSONObject;
import org.ruoyi.common.wechat.itchat4j.api.WechatTools;
import org.ruoyi.common.wechat.itchat4j.controller.LoginController;
import org.ruoyi.common.wechat.itchat4j.core.CoreManage;
import org.ruoyi.common.wechat.itchat4j.service.impl.LoginServiceImpl;
import org.ruoyi.common.wechat.itchat4j.utils.enums.SendMsgType;
import org.ruoyi.common.wechat.web.base.BaseException;

import java.io.IOException;
import java.util.List;

/**
 * @author WesleyOne
 * @create 2018/12/16
 */
public class RobotWorkController extends _BaseController {

    /**
     * 获取状态
     * @throws BaseException
     */
    public void getStatus() throws BaseException {
        String uniqueKey = getUniqueKey();
        boolean wechatStatus = WechatTools.getWechatStatus(uniqueKey);
        setData(wechatStatus);
        renderJson();
    }

    /**
     * 登录第一步，获取二维码链接
     * @throws BaseException
     */
    public void getQr() throws BaseException, IOException {
        String uniqueKey = getUniqueKey();
        LoginController login = new LoginController(uniqueKey);
        String qrSrc = login.login_1();
        setData(qrSrc);
        renderJson();
    }

    /**
     * 登录第二步,确认登录状态
     * @throws BaseException
     */
    public void login() throws BaseException {
        String uniqueKey = getUniqueKey();
        LoginController login = new LoginController(uniqueKey);
        boolean result = login.login_2();
        if (result){

        }else{
            setOperateErr();
        }
        renderJson();
    }

    /**
     * 登录第三步，确认登录及初始化信息
     * @throws BaseException
     */
    public void init() throws BaseException {
        String uniqueKey = getUniqueKey();
        LoginController login = new LoginController(uniqueKey);
        boolean loginResult = login.login_3();
        if (loginResult){
            setMsg("登录成功");
        }else{
            setMsg("登录失败，关闭二维码后重新打开");
        }
        renderJson();
    }

    /**
     * 退出
     * @throws BaseException
     */
    public void logout() throws BaseException {
        String uniqueKey = getUniqueKey();
        WechatTools.logout(uniqueKey);
        renderJson();
    }

    /**
     * 获取群昵称
     */
    public void getGroupNickNames() throws BaseException {
        String uniqueKey = getUniqueKey();
        List<String> groupNickNameList = WechatTools.getGroupNickNameList(uniqueKey);
        setData(groupNickNameList);
        renderJson();
    }

    /**
     * 获取好友昵称
     */
    public void getContactNickNames() throws BaseException {
        String uniqueKey = getUniqueKey();
        List<String> contactNickNameList = WechatTools.getContactNickNameList(uniqueKey);
        setData(contactNickNameList);
        renderJson();
    }

    public void getGroups() throws BaseException {
        String uniqueKey = getUniqueKey();
        List<JSONObject> groupList = WechatTools.getGroupList(uniqueKey);
        setData(groupList);
        renderJson();
    }

    public void getContacts() throws BaseException {
        String uniqueKey = getUniqueKey();
        List<JSONObject> contactList = WechatTools.getContactList(uniqueKey);
        setData(contactList);
        renderJson();
    }

    public void getCore() throws BaseException {
        String uniqueKey = getUniqueKey();
        setData(CoreManage.getInstance(uniqueKey));
        renderJson();
    }

    /**
     * 强制刷新通讯录
     */
    public void gct() throws BaseException {
        String uniqueKey = getUniqueKey();
        LoginServiceImpl loginService = new LoginServiceImpl(uniqueKey);
        loginService.webWxGetContact();
        renderJson();
    }

    /**
     * 刷新通讯录详情
     * @throws BaseException
     */
    public void ggp() throws BaseException {
        String uniqueKey = getUniqueKey();
        LoginServiceImpl loginService = new LoginServiceImpl(uniqueKey);
        loginService.WebWxBatchGetContact();
        renderJson();
    }

    /**
     * 重启
     * @throws BaseException
     */
    public void reboot() throws BaseException {
        String uniqueKey = getUniqueKey();
        LoginController login = new LoginController(uniqueKey);
        boolean loginResult = login.reboot();
        setData(loginResult);
        renderJson();
    }


    /**
     * 手动备份热登录信息
     */
    public void manualCopy(){
        CoreManage.persistence();
        renderJson();
    }

    public void testSend() throws BaseException {

        JSONObject postParam = getPostParam();
        String uniqueKey = postParam.getString("uniqueKey");
        String data = postParam.getString("valueData");
        String userName = postParam.getString("userName");
        String typeData = postParam.getString("typeData");

        CoreManage.addSendMsg4UserName(uniqueKey,userName,data, SendMsgType.fromValue(typeData));
        renderJson();
    }

}

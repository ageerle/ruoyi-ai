package org.ruoyi.common.wechat.web.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.render.JsonRender;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.wechat.itchat4j.core.Core;
import org.ruoyi.common.wechat.itchat4j.core.CoreManage;
import org.ruoyi.common.wechat.web.base.BaseException;
import org.ruoyi.common.wechat.web.model.WxRobConfig;
import org.ruoyi.common.wechat.web.utils.UUIDShortUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 机器人相关管理接口
 * @author WesleyOne
 * @create 2018/12/16
 */
public class RobotController extends _BaseController {

    /**
     * 机器人页面
     */
    public void index(){
        renderTemplate("index.html");
    }

    /**
     * 机器人列表
     * 分页，搜索条件
     */
    public void list(){
        int rows = getParaToInt("limit", 10);
        int pageNum = getPageNum(getParaToInt("offset", 1), rows);
        String searchUniqueKey = getPara("uniqueKey");
        String remark = getPara("remark");
        Boolean enable = getParaToBoolean("enable");
        String where = " where 1=1 ";
        if (StringUtils.isNotEmpty(searchUniqueKey)){
            where += " and unique_key = '"+searchUniqueKey + "' ";
        }
        if (StringUtils.isNotEmpty(remark)) {
            where += " and remark like '" + remark + "%' ";
        }
        if (enable != null){
            where += " and enable = " + (enable?1:0);
        }

        Page<WxRobConfig> page = WxRobConfig.dao.paginate(pageNum, rows, "select * ",
                " from wx_rob_config "+where);
        //其他处理
        List<WxRobConfig> dataList = page.getList();
        if (CollectionUtil.isNotEmpty(dataList)){
            for (WxRobConfig conf: dataList){
                // 获取机器人状态
                String uniqueKey = conf.getUniqueKey();
                conf.setActive(CoreManage.isActive(uniqueKey));
            }
        }

        setAttrs(buildPagination(dataList, page.getTotalRow()));
        render(new JsonRender().forIE());
    }

    /**
     * 机器人页面
     */
    public void addIndex(){
        renderTemplate("addIndex.html");
    }

    /**
     * 添加机器人
     */
    public void addRob(){
        JSONObject postParam = getPostParam();
        String remark = postParam.getString("remark");

        if (vldParamNull(remark,"微信号不能为空")){
            return;
        }

        Record remarkRecord = Db.findFirst("SELECT remark FROM wx_rob_config WHERE remark = ? LIMIT 1", remark);
        if (vldParamNull(remarkRecord != null,"备注已存在")){
            return;
        }
        WxRobConfig bean = new WxRobConfig();
        bean.setRemark(remark)
                .setCreateTime(new Date())
                .setUpdateTime(new Date())
                .setToFriend(false)
                .setToGroup(true)
                .setFromOut(false)
                .setDefaultFriend(false)
                .setDefaultGroup(false);

        boolean isSuccess = false;
        int maxTime = 5;
        while (!isSuccess && maxTime >0){
            String uniKey = UUIDShortUtil.generateShortUuid();
            bean.setUniqueKey(uniKey);
            isSuccess = bean.save();
            maxTime--;
        }

        if (!isSuccess){
            setOperateErr();
        }else{
            setData(bean);
        }
        renderJson();
    }

    /**
     * 机器人启动禁止开关，发送群聊开关，发送好友开关,对外接口消息开关
     */
    public void change(){
        JSONObject postParam = getPostParam();
        Long id = postParam.getLong("rid");
        String type = postParam.getString("type");
        Boolean state = postParam.getBoolean("state");

        if (id == null || StringUtils.isEmpty(type) || state == null){
            setOperateErr();
            renderJson();
            return;
        }

        WxRobConfig config = new WxRobConfig();
        config.setId(id);
        if ("enable".equals(type)){
            config.setEnable(state);
        }else if ("tofrd".equals(type)){
            config.setToFriend(state);
        }else if ("togrp".equals(type)){
            config.setToGroup(state);
        }else if ("fromout".equals(type)) {
            config.setFromOut(state);
        }else if ("default_group".equals(type)){
            config.setDefaultGroup(state);
        }else if ("default_friend".equals(type)){
            config.setDefaultFriend(state);
        }else {
            setOperateErr("非法操作");
            render(new JsonRender().forIE());
            return;
        }

        config.setUpdateTime(new Date());
        boolean update = config.update();
        if (!update){
            setOperateErr();
        }else{
            setData(update);
        }
        renderJson();
    }

    /**
     * 修改备注
     */
    public void changeRemark(){
        JSONObject postParam = getPostParam();
        Long id = postParam.getLong("rid");
        String remark = postParam.getString("remark");
        if (vldParamNull(id,"ID不能为空")){
            return;
        }
        if (vldParamNull(remark,"备注不能为空")){
            return;
        }

        Record remarkRecord = Db.findFirst("SELECT remark FROM wx_rob_config WHERE remark = ? LIMIT 1", remark);
        if (vldParamNull(remarkRecord != null,"备注已存在")){
            return;
        }
        WxRobConfig config = new WxRobConfig();
        config.setId(id);
        config.setRemark(remark);
        boolean update = config.update();
        if (!update){
            setOperateErr();
        }else{
            setData(update);
        }
        renderJson();
    }

    /**
     * 发送页面
     */
    public void sendIndex() throws BaseException {
        String uniqueKey = getUniqueKey();
        Core core = CoreManage.getInstance(uniqueKey);
        List<JSONObject> sourceSendList = new ArrayList<>();
        sourceSendList.addAll( core.getGroupList());
        sourceSendList.addAll( core.getContactList());
        List<JSONObject> targetList = new ArrayList<>();
        JSONObject filehelper = new JSONObject();
        filehelper.put("UserName","filehelper");
        filehelper.put("NickName","文件传输助手");
        targetList.add(filehelper);
        for (JSONObject jsonObject : sourceSendList) {
            JSONObject newObject = new JSONObject();
            if (StringUtils.isEmpty(jsonObject.getString("NickName"))){
                continue;
            }
            newObject.put("NickName",jsonObject.getString("NickName"));
            newObject.put("UserName",jsonObject.getString("UserName"));
            targetList.add(newObject);
        }
        setAttr("uniqueKey",uniqueKey);
        setAttr("targetList",targetList);
        renderTemplate("sendIndex.html");
    }

}

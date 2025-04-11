package org.ruoyi.common.wechat.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.render.JsonRender;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.wechat.itchat4j.api.WechatTools;
import org.ruoyi.common.wechat.web.model.WxRobRelation;
import org.ruoyi.common.wechat.web.utils.UUIDShortUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * TODO 关联配置
 * @author WesleyOne
 * @create 2018/12/16
 */
public class RelateController extends _BaseController {

    public void index(){
        String outKey = getPara("ok");
        if (StringUtils.isNotEmpty(outKey)){
            setAttr("search_ok",outKey);
        }
        String searchUk = getPara("uk");
        if (StringUtils.isNotEmpty(searchUk)){
            setAttr("search_uk",searchUk);
        }
        renderTemplate("index.html");
    }

    public void list(){
        int rows = getParaToInt("limit", 10);
        int pageNum = getPageNum(getParaToInt("offset", 1), rows);
        String outKey = getPara("outKey");
        String uniqueKey = getPara("uniqueKey");
        String nickName = getPara("nickName");
        Boolean enable = getParaToBoolean("enable");
        Boolean togrp = getParaToBoolean("togrp");

        String where = " where 1=1 ";
        if (StringUtils.isNotEmpty(outKey)) {
            where += " and out_key = '" + outKey + "' ";
        }
        if (StringUtils.isNotEmpty(uniqueKey)){
            where += " and unique_key = '"+uniqueKey + "' ";
        }
        if (StringUtils.isNotEmpty(nickName)) {
            where += " and nick_name LIKE '" + nickName + "%' ";
        }
        if (enable != null){
            where += " and enable = " + (enable?1:0);
        }
        if (togrp != null){
            where += " and to_group = " + (togrp?1:0);
        }

        Page<WxRobRelation> page = WxRobRelation.dao.paginate(pageNum, rows, "select * ",
                " from wx_rob_relation "+where);

        setAttrs(buildPagination(page.getList(), page.getTotalRow()));
        render(new JsonRender().forIE());
    }

    public void editIndex(){
        Integer kid = getParaToInt("kid");
        WxRobRelation wxRobRelation;
        boolean isEdit = true;
        List<String> groupNickNames = new ArrayList<>();
        if (kid != null){
            wxRobRelation = WxRobRelation.dao.findById(kid);
        }else{
            isEdit = false;
            wxRobRelation = new WxRobRelation();
            String uniqueKey = getPara("uk");
            String nickName = getPara("nk");
            Boolean toGroup = getParaToBoolean("tgb");
            if (StringUtils.isNotEmpty(uniqueKey)){
                wxRobRelation.setUniqueKey(uniqueKey);
                groupNickNames.addAll(WechatTools.getGroupNickNameList(uniqueKey));
            }
            if (StringUtils.isNotEmpty(nickName)){
                wxRobRelation.setNickName(nickName);
            }
            if (toGroup != null){
                wxRobRelation.setToGroup(toGroup);
            }else{
                wxRobRelation.setToGroup(true);
            }
        }
        setAttr("isEdit",isEdit);
        setAttr("form",wxRobRelation);
        setAttr("groupNickNames",groupNickNames);
        renderTemplate("editIndex.html");
    }

    /**
     * 编辑外部id关联
     */
    public void editRelate(){
        JSONObject postParam = getPostParam();
        Long id = postParam.getLong("kid");
        String uniqueKey = postParam.getString("uniqueKey");
        String nickName = postParam.getString("nickName");
        String whiteList = postParam.getString("whiteList");
        Boolean enable = postParam.getBoolean("enable");
        Boolean toGroup = postParam.getBoolean("toGroup");

        WxRobRelation editRecord = new WxRobRelation();

        if (StringUtils.isNotEmpty(nickName)){
            editRecord.setNickName(nickName);
        }
        if (enable != null){
            editRecord.setEnable(enable);
        }
        if (toGroup != null){
            editRecord.setToGroup(toGroup);
        }
        if (StringUtils.isNotEmpty(whiteList)){
            editRecord.setWhiteList(whiteList);
        }

        if (id != null){
            editRecord.setId(id);
            boolean update = editRecord.update();
            if (update){
                setMsg("修改成功");
            }else{
                setOperateErr("修改失败");
            }
        }else{
            // 校验
            editRecord.setUniqueKey(uniqueKey);
            editRecord.setCreateTime(new Date());
            editRecord.setEnable(true);
            if (vldParamNull(editRecord.getUniqueKey(),"唯一码不能为空")){
                return;
            }
            if (vldParamNull(editRecord.getNickName(),"昵称不能为空")){
                return;
            }
            if (vldParamNull(editRecord.getToGroup(),"群聊好友未选择")){
                return;
            }

            boolean isSuccess = false;
            int maxTime = 5;
            while (!isSuccess && maxTime >0){
                String outKey = UUIDShortUtil.generateShortUuid();
                editRecord.setOutKey(outKey);
                isSuccess = editRecord.save();
                maxTime--;
            }
            if (isSuccess){
                setMsg("新增成功");
            }else{
                setOperateErr("新增失败");
            }
        }
        renderJson();
    }

    /**
     * 删除外部id关联
     */
    public void delRelate(){
        String kid = getPara("kid");
        boolean delete = WxRobRelation.dao.deleteById(kid);
        if (delete){
            setMsg("删除成功");
        }else{
            setOperateErr("删除失败");
        }
        renderJson();
    }
}

package org.ruoyi.common.wechat.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.render.JsonRender;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.wechat.itchat4j.api.WechatTools;
import org.ruoyi.common.wechat.web.enums.KeyMsgValueType;
import org.ruoyi.common.wechat.web.model.WxRobKeyword;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * TODO 关键词操作
 * @author WesleyOne
 * @create 2018/12/16
 */
public class KeyWordController extends _BaseController{

    public void index(){

        String uniqueKey = getPara("uk");
        if (StringUtils.isNotEmpty(uniqueKey)){
            setAttr("search_uk",uniqueKey);
        }
        setAttr("keys",KeyMsgValueType.LIST_KV);
        renderTemplate("index.html");
    }


    public void list(){
        int rows = getParaToInt("limit", 10);
        int pageNum = getPageNum(getParaToInt("offset", 1), rows);
        String uniqueKey = getPara("uniqueKey");
        String keyData = getPara("keyData");
        String nickName = getPara("nickName");
        String typeData = getPara("typeData");
        Boolean enable = getParaToBoolean("enable");
        Boolean togrp = getParaToBoolean("togrp");

        String where = " where 1=1 ";
        if (StringUtils.isNotEmpty(uniqueKey)){
            where += " and unique_key = '"+uniqueKey + "' ";
        }
        if (StringUtils.isNotEmpty(keyData)) {
            where += " and key_data LIKE '" + keyData + "%' ";
        }
        if (StringUtils.isNotEmpty(nickName)) {
            where += " and nick_name LIKE '" + nickName + "%' ";
        }
        if (KeyMsgValueType.fromValue(typeData) != null){
            where += " and type_data = '" + typeData + "' ";
        }
        if (enable != null){

            where += " and enable = " + (enable?1:0);
        }
        if (togrp != null){
            where += " and to_group = " + (togrp?1:0);
        }

        Page<WxRobKeyword> page = WxRobKeyword.dao.paginate(pageNum, rows, "select * ",
                " from wx_rob_keyword "+where);

        setAttrs(buildPagination(page.getList(), page.getTotalRow()));
        render(new JsonRender().forIE());
    }

    public void editIndex(){
        Integer kid = getParaToInt("kid");
        WxRobKeyword kwRecord;
        boolean isEdit = true;
        List<String> groupNickNames = new ArrayList<>();
        if (kid != null){
            kwRecord = WxRobKeyword.dao.findById(kid);
        }else{
            isEdit = false;
            kwRecord = new WxRobKeyword();
            String uniqueKey = getPara("uk");
            if (StringUtils.isNotEmpty(uniqueKey)){
                kwRecord.setUniqueKey(uniqueKey);
                groupNickNames.addAll(WechatTools.getGroupNickNameList(uniqueKey));
            }
            // 默认显示文本
            kwRecord.setTypeData(KeyMsgValueType.TEXT.toValue());
        }
        setAttr("isEdit",isEdit);
        setAttr("form",kwRecord);

        setAttr("keys",KeyMsgValueType.LIST_KV);
        setAttr("groupNickNames",groupNickNames);
        renderTemplate("editIndex.html");
    }

    /**
     * 新增修改
     */
    public void editKeyWord(){
        JSONObject postParam = getPostParam();
        Long id = postParam.getLong("kid");
        String uniqueKey = postParam.getString("uniqueKey");
        String keyData = postParam.getString("keyData");
        String valueData = postParam.getString("valueData");
        String nickName = postParam.getString("nickName");
        String typeData = postParam.getString("typeData");
        Boolean enable = postParam.getBoolean("enable");
        Boolean toGroup = postParam.getBoolean("toGroup");

        WxRobKeyword editRecord = new WxRobKeyword();

        if (StringUtils.isNotEmpty(keyData)){
            editRecord.setKeyData(keyData);
        }
        if (StringUtils.isNotEmpty(valueData)){
            editRecord.setValueData(valueData);
        }
        if (StringUtils.isNotEmpty(nickName)){
            editRecord.setNickName(nickName);
        }
        if (KeyMsgValueType.fromValue(typeData) != null){
            editRecord.setTypeData(typeData);
        }else {
            editRecord.setTypeData(KeyMsgValueType.TEXT.toValue());
        }
        if (enable != null){
            editRecord.setEnable(enable);
        }
        if (toGroup != null){
            editRecord.setToGroup(toGroup);
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
            if (vldParamNull(editRecord.getKeyData(),"关键字不能为空")){
                return;
            }
            if (vldParamNull(editRecord.getValueData(),"内容不能为空")){
                return;
            }
            if (vldParamNull(editRecord.getTypeData(),"内容类型不能为空")){
                return;
            }
            if (vldParamNull(editRecord.getNickName(),"昵称不能为空")){
                return;
            }
            if (vldParamNull(editRecord.getToGroup(),"群聊好友未选择")){
                return;
            }
            boolean save = editRecord.save();
            if (save){
                setMsg("新增成功");
            }else{
                setOperateErr("新增失败");
            }
        }
        renderJson();
    }

    /**
     * 删除关键字
     */
    public void delKeyWord(){

        String kid = getPara("kid");
        boolean delete = WxRobKeyword.dao.deleteById(kid);
        if (delete){
            setMsg("删除成功");
        }else{
            setOperateErr("删除失败");
        }
        renderJson();
    }


}

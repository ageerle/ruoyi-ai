package org.ruoyi.common.wechat.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.core.Controller;
import com.jfinal.core.NotAction;
import com.jfinal.kit.HttpKit;
import com.jfinal.kit.StrKit;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.wechat.web.base.BaseError;
import org.ruoyi.common.wechat.web.base.BaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author WesleyOne
 * @create 2018/7/27
 */
public class _BaseController extends Controller {

    public final Logger LOG = LoggerFactory.getLogger(this.getClass());


    /**
     * 通用验证
     * @param result    true说明验证不通过
     * @param code
     * @param errorMsg
     */
    @NotAction
    public boolean vldParam(boolean result, String code, String errorMsg){
        if (result){
            setAttr("code",code);
            setAttr("message",errorMsg);
            this.renderJson();
            return true;
        }
        return false;
    }

    @NotAction
    public boolean vldParam(boolean result, BaseError baseError){
        if (result){
            return vldParam(true,baseError.getCode(),baseError.getMsg());
        }
        return false;
    }

    @NotAction
    public boolean vldParam(boolean result, String errMsg){
        if (result){
            return vldParam(true,BaseError.NORMAL_ERR.getCode(),errMsg);
        }
        return false;
    }

    /**
     * 参数非空判断
     * @param paramValue
     * @param baseError
     * @return
     */
    @NotAction
    public boolean vldParamNull(String paramValue, BaseError baseError){
        if (StrKit.isBlank(paramValue)) {
            return vldParam(true,baseError.getCode(),baseError.getMsg());
        }
        return false;
    }

    @NotAction
    public boolean vldParamNull(String paramValue, String errMsg){
        if (StrKit.isBlank(paramValue)) {
            return vldParam(true,BaseError.NORMAL_ERR.getCode(),errMsg);
        }
        return false;
    }

    /**
     * 参数非空判断
     * @param paramValue
     * @param baseError
     * @return
     */
    @NotAction
    public boolean vldParamNull(Object paramValue, BaseError baseError){
        if (paramValue == null) {
            return vldParam(true,baseError.getCode(),baseError.getMsg());
        }
        return false;
    }

    @NotAction
    public boolean vldParamNull(Object paramValue, String errMsg){
        if (paramValue == null) {
            return vldParam(true,BaseError.NORMAL_ERR.getCode(),errMsg);
        }
        return false;
    }

    @NotAction
    public void setData(Object o){
        this.setAttr("data",o);
    }
    @NotAction
    public void setCount(Object o){
        this.setAttr("_count",o);
    }
    @NotAction
    public void setCode(String code){
        this.setAttr("code",code);
    }
    @NotAction
    public void setMsg(String msg){
        this.setAttr("message",msg);
    }
    @NotAction
    public void setOperateErr(String msg){
        this.setAttr("code",BaseError.OPERATION_ERR.getCode());
        this.setAttr("message",msg);
    }
    @NotAction
    public void setOperateErr(){
        this.setAttr("code",BaseError.OPERATION_ERR.getCode());
        this.setAttr("message",BaseError.OPERATION_ERR.getMsg());
    }
    @NotAction
    public void setDeleteErr(){
        this.setAttr("code",BaseError.OPERATION_ERR.getCode());
        this.setAttr("message",BaseError.OPERATION_ERR.getMsg());
    }

    @NotAction
    public void addCookie(String key,String value,int second) {
        Cookie cookie = new Cookie(key,value);
        cookie.setMaxAge(second);
        cookie.setPath("/");
        setCookie(cookie);
    }

    @NotAction
    public JSONObject getPostParam(){
        String jsonString= HttpKit.readData(getRequest());
        return JSONObject.parseObject(jsonString);
    }
    @NotAction
    public String getUid(){
        return this.getCookie("uid");
    }

    /**
     * 分页处理
     * @param list
     * @param count
     * @return
     */
    @SuppressWarnings("rawtypes")
    @NotAction
    protected Map<String, Object> buildPagination(List list, Integer count) {
        return buildPagination(list, count, null);
    }

    @SuppressWarnings("rawtypes")
    @NotAction
    protected Map<String, Object> buildPagination(List list, Integer count,
                                                  List<Map<String, Object>> footer) {
        Map<String, Object> map = new HashMap<String, Object>(4);
        map.put("total", count);
        map.put("rows", list);
        if (footer != null){
            map.put("footer", footer);
        }
        return map;
    }

    @NotAction
    protected static int getPageNum(int pageNum,int rows){
        int pageNumber = pageNum / rows + 1;
        return pageNumber;
    }

    /**
     * 获取UniqueKey
     * @return
     */
    @NotAction
    public String getUniqueKey() throws BaseException {
        String uniqueKey = getPara("_ck", "");
        if (StringUtils.isEmpty(uniqueKey)){
            throw new BaseException("机器唯一码为空");
        }
        return uniqueKey;
    }

}

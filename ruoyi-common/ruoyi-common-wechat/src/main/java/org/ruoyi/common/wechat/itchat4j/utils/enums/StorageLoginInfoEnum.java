package org.ruoyi.common.wechat.itchat4j.utils.enums;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * Created by xiaoxiaomo on 2017/5/7.
 */
public enum StorageLoginInfoEnum {

    //URL
    url("url",new String()),
    fileUrl("fileUrl",new String()),
    syncUrl("syncUrl",new String()),
    //生成15位随机数
    deviceid("deviceid",new String()),

    //baseRequest
    skey("skey",new String()),
    wxsid("wxsid",new String()),
    wxuin("wxuin",new String()),
    pass_ticket("pass_ticket",new String()),


    InviteStartCount("InviteStartCount",new Integer(0)),
    // 登录用户登录时信息
    User("User",new JSONObject()),
    SyncKey("SyncKey",new JSONObject()),
    synckey("synckey",new String()),



    MemberCount("MemberCount",new String()),
    MemberList("MemberList",new JSONArray()),



    ;

    private String key;
    private Object type;

    StorageLoginInfoEnum(String key, Object type) {
        this.key = key;
        this.type = type;
    }

    public String getKey() {
        return key;
    }


    public Object getType() {
        return type;
    }

}

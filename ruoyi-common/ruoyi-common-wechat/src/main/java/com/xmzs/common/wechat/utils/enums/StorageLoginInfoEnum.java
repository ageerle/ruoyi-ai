package com.xmzs.common.wechat.utils.enums;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xiaoxiaomo on 2017/5/7.
 */
public enum StorageLoginInfoEnum {

    //URL
    url("url",new String()),
    fileUrl("fileUrl",new String()),
    syncUrl("syncUrl",new String()),

    deviceid("deviceid",new String()), //生成15位随机数

    //baseRequest
    skey("skey",new String()),
    wxsid("wxsid",new String()),
    wxuin("wxuin",new String()),
    pass_ticket("pass_ticket",new String()),


    InviteStartCount("InviteStartCount",0),
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

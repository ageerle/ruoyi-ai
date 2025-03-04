package org.ruoyi.common.wechat.web.cache;

import org.ruoyi.common.wechat.web.utils.LRUCache;

import java.util.LinkedHashMap;

/**
 * 简单的本地会话存储
 * @author WesleyOne
 * @create 2018/9/25
 */
public class UserSession {
    public static LinkedHashMap<String,String> USERSESSION_CACHE = new LRUCache<String, String>(64);

    public static void addUserSession(String username,String userSession){
        USERSESSION_CACHE.put(username,userSession);
    }

    public static void delUserSession(String username){
        USERSESSION_CACHE.remove(username);
    }

    public static boolean checkUserSession(String username,String userSession){
        String s = USERSESSION_CACHE.get(username);
        if (userSession!=null&&userSession.equals(s)){
            return true;
        }
        return false;
    }
}

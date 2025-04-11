package org.ruoyi.common.wechat.itchat4j.utils.enums;


import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author WesleyOne
 * @create 2018/12/21
 */
public enum SelectorEnum {


    UNKOWN("9999", "未知"),
    NORMAL("0", "正常"),
    NEW_MSG("2", "有新消息"),
    SELECTOR_3("3", "访问频繁#"),
    MOD_CONTACT("4", "有人修改了自己的昵称或你修改了别人的备注"),
    ADD_OR_DEL_CONTACT("6", "存在删除或者新增的好友信息"),
    ENTER_OR_LEAVE_CHAT("7", "进入或离开聊天界面");

    private static final Map<String, SelectorEnum> lookup = new HashMap<>();
    static {
        for (SelectorEnum s : EnumSet.allOf(SelectorEnum.class)) {
            lookup.put(s.getCode(), s);
        }
    }
    public static SelectorEnum fromCode(String code) {
        return lookup.get(code);
    }

    private String code;
    private String type;

    SelectorEnum(String code, String type) {
        this.code = code;
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }
}

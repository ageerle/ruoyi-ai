package org.ruoyi.common.chat.domain.vo.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.BeanUtils;

/** 模型管理编辑详情，允许有查询权限的管理员回显已保存的密钥。 */
public class ChatModelDetailVo extends ChatModelVo {

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    public String getApiKey() {
        return super.getApiKey();
    }

    public static ChatModelDetailVo from(ChatModelVo model) {
        if (model == null) {
            return null;
        }
        ChatModelDetailVo detail = new ChatModelDetailVo();
        BeanUtils.copyProperties(model, detail);
        return detail;
    }
}

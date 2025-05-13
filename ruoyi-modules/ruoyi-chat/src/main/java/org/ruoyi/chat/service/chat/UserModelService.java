package org.ruoyi.chat.service.chat;

import lombok.RequiredArgsConstructor;
import org.ruoyi.chat.enums.DisplayType;
import org.ruoyi.domain.bo.ChatModelBo;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatModelService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户模型信息
 *
 * @author ageerle@163.com
 * date 2025/4/10
 */

@Service
@RequiredArgsConstructor
public class UserModelService {

    private final IChatModelService chatModelService;

    public List<ChatModelVo> modelList(ChatModelBo bo) {
        bo.setModelShow(DisplayType.VISIBLE.getCode());
        return chatModelService.queryList(bo);
    }

}

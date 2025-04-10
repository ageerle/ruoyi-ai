package org.ruoyi.chat.service.chat;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.ruoyi.chat.enums.DisplayType;
import org.ruoyi.chat.enums.UserGradeType;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.domain.bo.ChatModelBo;
import org.ruoyi.domain.bo.ChatPackagePlanBo;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.domain.vo.ChatPackagePlanVo;
import org.ruoyi.service.IChatModelService;
import org.ruoyi.service.IChatPackagePlanService;
import org.ruoyi.system.domain.vo.SysUserVo;
import org.ruoyi.system.service.ISysUserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 描述：用户模型信息
 *
 * @author ageerle@163.com
 * date 2025/4/10
 */

@Service
@RequiredArgsConstructor
public class UserModelService {

    private final IChatModelService chatModelService;

    private final ISysUserService userService;

    private final IChatPackagePlanService packagePlanService;

    public List<ChatModelVo> modelList(ChatModelBo bo) {
        bo.setModelShow(DisplayType.VISIBLE.getCode());
        List<ChatModelVo> chatModelList = chatModelService.queryList(bo);
        ChatPackagePlanBo sysPackagePlanBo = new ChatPackagePlanBo();
        if (StpUtil.isLogin()) {
            Long userId = LoginHelper.getLoginUser().getUserId();
            SysUserVo sysUserVo = userService.selectUserById(userId);
            if (UserGradeType.UNPAID.getCode().equals(sysUserVo.getUserGrade())){
                sysPackagePlanBo.setName("Free");
                ChatPackagePlanVo chatPackagePlanVo = packagePlanService.queryList(sysPackagePlanBo).get(0);
                List<String> array = new ArrayList<>(Arrays.asList(chatPackagePlanVo.getPlanDetail().split(",")));
                chatModelList.removeIf(model -> !array.contains(model.getModelName()));
            }
        }else {
            sysPackagePlanBo.setName("Visitor");
            ChatPackagePlanVo sysPackagePlanVo = packagePlanService.queryList(sysPackagePlanBo).get(0);
            List<String> array = new ArrayList<>(Arrays.asList(sysPackagePlanVo.getPlanDetail().split(",")));
            chatModelList.removeIf(model -> !array.contains(model.getModelName()));
        }
        return new ArrayList<>(chatModelList);
    }

}

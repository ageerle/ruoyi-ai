package org.ruoyi.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.bo.ChatConfigBo;
import org.ruoyi.system.domain.vo.ChatConfigVo;
import org.ruoyi.system.service.IChatConfigService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对话配置信息
 *
 * @author Lion Li
 * @date 2024-04-13
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/chat/config")
public class ChatConfigController extends BaseController {

    private final IChatConfigService chatConfigService;

    private final ConfigService configService;

    /**
     * 查询配置信息列表
     */
    @GetMapping("/list")
    @SaCheckPermission("system:config:list")
    public List<ChatConfigVo> list(ChatConfigBo bo) {
        return chatConfigService.queryList(bo);
    }

    /**
     * 获取对话配置信详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<ChatConfigVo> getInfo(@NotNull(message = "主键不能为空")
                                   @PathVariable Long id) {
        return R.ok(chatConfigService.queryById(id));
    }

    /**
     * 根据参数键名查询系统参数值
     *
     * @param configKey 参数Key
     */
    @GetMapping(value = "/configKey/{configKey}")
    public R<String> getConfigKey(@PathVariable String configKey) {
        return R.ok(configService.getConfigValue("sys",configKey));
    }

    /**
     * 查询系统参数
     *
     */
    @GetMapping(value = "/sysConfigKey")
    public R<List<ChatConfigVo>> getSysConfigKey() {
        return R.ok(chatConfigService.getSysConfigValue("sys"));
    }

    /**
     * 新增对话配置信息
     */
    @PostMapping("/add")
    public R<Void> add(@RequestBody List<ChatConfigBo> boList) {
        for (ChatConfigBo chatConfigBo : boList) {
            if(chatConfigBo.getId() == null){
                chatConfigService.insertByBo(chatConfigBo);
            }else {
                chatConfigService.updateByBo(chatConfigBo);
            }
        }
        return toAjax(true);
    }

    /**
     * 修改对话配置信息
     */
    @PutMapping("/edit")
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ChatConfigBo bo) {
        return toAjax(chatConfigService.updateByBo(bo));
    }

    /**
     * 删除对话配置信息
     *
     * @param ids 主键串
     */
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(chatConfigService.deleteWithValidByIds(List.of(ids), true));
    }


}

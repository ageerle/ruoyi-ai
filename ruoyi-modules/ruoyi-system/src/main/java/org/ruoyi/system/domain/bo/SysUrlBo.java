package org.ruoyi.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.system.domain.SysUrl;

/**
 * URL 管理业务对象 sys_url
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysUrl.class, reverseConvertGenerate = false)
public class SysUrlBo extends BaseEntity {

    /**
     * 链接ID
     */
    @NotNull(message = "链接ID不能为空", groups = {EditGroup.class})
    private Long urlId;

    /**
     * 链接名称
     */
    @NotBlank(message = "链接名称不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 100, message = "链接名称不能超过{max}个字符", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /**
     * HTTP(S) 地址
     */
    @NotBlank(message = "链接地址不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 500, message = "链接地址不能超过{max}个字符", groups = {AddGroup.class, EditGroup.class})
    @Pattern(regexp = "^https?://.*", message = "链接地址必须以 http:// 或 https:// 开头", groups = {AddGroup.class, EditGroup.class})
    private String url;

    /**
     * 链接说明
     */
    @Size(max = 500, message = "链接说明不能超过{max}个字符", groups = {AddGroup.class, EditGroup.class})
    private String description;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态（0正常 1停用）
     */
    @NotBlank(message = "状态不能为空", groups = {AddGroup.class, EditGroup.class})
    @Pattern(regexp = "^[01]$", message = "状态只能为 0（正常）或 1（停用）", groups = {AddGroup.class, EditGroup.class})
    private String status;

    /**
     * 备注
     */
    private String remark;

}

package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * OSS 文件实体（[SEC-FIX-HIGH-1.1-FOLLOWUP]）。
 * <p>共享 sys_oss 表（IPD 模块独立映射，避免与 ruoyi-system 模块横向依赖）。
 * 仅读取 url/fileName 字段用于 Gate 强制输出物守卫。
 */
@Data
@TableName(value = "sys_oss")
public class OssFileEntity implements Serializable {

    @TableId(value = "oss_id", type = IdType.ASSIGN_ID)
    private Long ossId;

    /** 访问 URL（resolveOssUrl 读取此字段写入 Gate.materialsUrl/meetingMinutesUrl）。 */
    private String url;

    private String fileName;

    private String originalName;

    private String fileSuffix;

    private String service;

    private String createBy;

    private Date createTime;
}

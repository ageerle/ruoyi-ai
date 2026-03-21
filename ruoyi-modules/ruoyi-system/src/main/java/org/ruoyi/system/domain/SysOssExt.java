package org.ruoyi.system.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 附件扩展字段对象（存储在 SysOss.ext1 的 JSON 字符串中）
 *
 * @author AprilWind
 */
@Data
public class SysOssExt implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 所属业务类型（如 avatar、report、contract）
     */
    private String bizType;

    /**
     * 文件大小（单位：字节）
     */
    private Long fileSize;

    /**
     * 文件类型（MIME类型，如 image/png）
     */
    private String contentType;

    /**
     * 来源标识（如 userUpload、systemImport）
     */
    private String source;

    /**
     * 上传 IP 地址，便于审计和追踪
     */
    private String uploadIp;

    /**
     * 附件说明或备注
     */
    private String remark;

    /**
     * 附件标签，如 ["图片", "证件"]
     */
    private List<String> tags;

    /**
     * 业务绑定ID（如某业务记录ID）
     */
    private String refId;

    /**
     * 绑定业务类型
     */
    private String refType;

    /**
     * 是否为临时文件，用于区分正式或待清理
     */
    private Boolean isTemp;

    /**
     * 文件MD5值（可用于去重或校验）
     */
    private String md5;

}

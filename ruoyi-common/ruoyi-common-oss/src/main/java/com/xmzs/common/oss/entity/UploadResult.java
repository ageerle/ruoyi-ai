package com.xmzs.common.oss.entity;

import lombok.Builder;
import lombok.Data;

/**
 * 上传返回体
 *
 * @author Lion Li
 */
@Data
@Builder
public class UploadResult {

    /**
     * 文件路径
     */
    private String url;

    /**
     * 文件名
     */
    private String filename;
}

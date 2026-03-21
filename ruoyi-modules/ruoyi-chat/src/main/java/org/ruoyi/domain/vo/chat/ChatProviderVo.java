package org.ruoyi.domain.vo.chat;

import org.ruoyi.domain.entity.chat.ChatProvider;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;



/**
 * 厂商管理视图对象 chat_provider
 *
 * @author ageerle
 * @date 2025-12-14
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ChatProvider.class)
public class ChatProviderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 厂商名称
     */
    @ExcelProperty(value = "厂商名称")
    private String providerName;

    /**
     * 厂商编码
     */
    @ExcelProperty(value = "厂商编码")
    private String providerCode;

    /**
     * 厂商图标
     */
    @ExcelProperty(value = "厂商图标")
    private String providerIcon;

    /**
     * 厂商描述
     */
    @ExcelProperty(value = "厂商描述")
    private String providerDesc;

    /**
     * API地址
     */
    @ExcelProperty(value = "API地址")
    private String apiHost;

    /**
     * 状态（0正常 1停用）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=正常,1=停用")
    private String status;

    /**
     * 排序
     */
    @ExcelProperty(value = "排序")
    private Long sortOrder;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 更新IP
     */
    @ExcelProperty(value = "更新IP")
    private String updateIp;


}

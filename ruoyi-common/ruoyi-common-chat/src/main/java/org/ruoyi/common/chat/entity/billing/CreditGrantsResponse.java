package org.ruoyi.common.chat.entity.billing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 余额查询接口返回值
 *
 * @author https:www.unfbx.com
 * @since 2023-03-18
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreditGrantsResponse implements Serializable {
    private String object;
    /**
     * 总金额：美元
     */
    @JsonProperty("total_granted")
    private BigDecimal totalGranted;
    /**
     * 总使用金额：美元
     */
    @JsonProperty("total_used")
    private BigDecimal totalUsed;
    /**
     * 总剩余金额：美元
     */
    @JsonProperty("total_available")
    private BigDecimal totalAvailable;
    /**
     * 余额明细
     */
    private Grants grants;
}

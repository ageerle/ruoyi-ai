package org.ruoyi.common.chat.entity.moderations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 描述：
 *
 * @author https:www.unfbx.com
 *  2023-02-15
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryScores implements Serializable {
    private BigDecimal hate;
    @JsonProperty("hate/threatening")
    private BigDecimal hateThreatening;
    @JsonProperty("self-harm")
    private BigDecimal selfHarm;
    private BigDecimal sexual;
    @JsonProperty("sexual/minors")
    private BigDecimal sexualMinors;
    private BigDecimal violence;
    @JsonProperty("violence/graphic")
    private BigDecimal violenceGraphic;

}

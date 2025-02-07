package org.ruoyi.common.chat.entity.fineTune;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HyperParam implements Serializable {

    @JsonProperty("batch_size")
    private Integer batchSize;
    @JsonProperty("learning_rate_multiplier")
    private Double learningRateMultiplier;
    @JsonProperty("n_epochs")
    private Integer nEpochs;
    @JsonProperty("prompt_loss_weight")
    private Double promptLossWeight;
}

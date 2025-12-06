package org.ruoyi.common.chat.entity.fineTune;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.openai.exception.CommonError;
import org.ruoyi.common.core.exception.base.BaseException;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Getter
@Slf4j
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class FineTune implements Serializable {

    /**
     * 上传的文件ID
     */
    @NonNull
    @JsonProperty("training_file")
    private String trainingFile;

    @JsonProperty("validation_file")
    private String validationFile;
    /**
     * 参考
     *
     * @see Model
     */
    private String model;

    @JsonProperty("n_epochs")
    @Builder.Default
    private Integer n_epochs = 4;

    @JsonProperty("batch_size")
    private Integer batchSize;

    @JsonProperty("learning_rate_multiplier")
    private Double learningRateMultiplier;

    @JsonProperty("prompt_loss_weight")
    @Builder.Default
    private Double promptLossWeight = 0.01;

    @JsonProperty("compute_classification_metrics")
    @Builder.Default
    private boolean computeClassificationMetrics = false;

    @JsonProperty("classification_n_classes")
    private Integer classificationNClasses;

    @JsonProperty("classification_betas")
    private List classificationBetas;

    private String suffix;

    public void setTrainingFile(String trainingFile) {
        this.trainingFile = trainingFile;
    }

    public void setValidationFile(String validationFile) {
        this.validationFile = validationFile;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setN_epochs(Integer n_epochs) {
        this.n_epochs = n_epochs;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public void setLearningRateMultiplier(Double learningRateMultiplier) {
        this.learningRateMultiplier = learningRateMultiplier;
    }

    public void setPromptLossWeight(Double promptLossWeight) {
        this.promptLossWeight = promptLossWeight;
    }

    public void setComputeClassificationMetrics(boolean computeClassificationMetrics) {
        this.computeClassificationMetrics = computeClassificationMetrics;
    }

    public void setClassificationNClasses(Integer classificationNClasses) {
        this.classificationNClasses = classificationNClasses;
    }

    public void setClassificationBetas(List classificationBetas) {
        this.classificationBetas = classificationBetas;
    }

    public void setSuffix(String suffix) {
        if (Objects.nonNull(suffix) && !"".equals(suffix) && suffix.length() > 40) {
            log.error("后缀长度不能大于40");
            throw new BaseException(CommonError.PARAM_ERROR.msg());
        }
        this.suffix = suffix;
    }

    @Getter
    @AllArgsConstructor
    public enum Model {
        // or a fine-tuned model created after 2022-04-21.
        ADA("ada"),
        BABBAGE("babbage"),
        CURIE("curie"),
        DAVINCI("davinci"),
        ;
        private String name;
    }
}

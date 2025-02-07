package org.ruoyi.common.chat.entity.fineTune;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FineTuneResponse implements Serializable {

    private String id;

    private String object;

    private String model;

    @JsonProperty("created_at")
    private long createdAt;

    private List<Event> events;

    @JsonProperty("fine_tuned_model")
    private String fineTunedModel;

    @JsonProperty("hyperparams")
    private HyperParam hyperParams;

    @JsonProperty("organization_id")
    private String organizationId;

    @JsonProperty("result_files")
    private List resultFiles;

    private String status;

    @JsonProperty("validation_files")
    private List validationFiles;

    @JsonProperty("training_files")
    private List<TrainingFile> trainingFiles;

    @JsonProperty("updated_at")
    private long updatedAt;


}

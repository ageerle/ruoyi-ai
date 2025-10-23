package org.ruoyi.workflow.workflow.node.classifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ClassifierNodeConfig {
    private List categories = new ArrayList<>();
    @JsonProperty("model_platform")
    private String modelPlatform;
    @JsonProperty("model_name")
    private String modelName;
}

package org.ruoyi.common.chat.entity.fineTune;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FineTuneDeleteResponse implements Serializable {

    private String id;

    private String object;

    private boolean deleted;

}

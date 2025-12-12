package org.ruoyi.common.chat.entity.files;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * @author https:www.unfbx.com
 * 2023-02-15
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class File implements Serializable {

//    private String id;
//    private String object;
//    private long bytes;
//    private long created_at;
//    private String filename;
//    private String purpose;
//    private String status;
//    @JsonProperty("status_details")
//    private String statusDetails;

    private long bytes;
    private long created_at;
    private String filename;
    private String id;
    private String object;
    private String url;
}

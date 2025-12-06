package org.ruoyi.common.chat.entity.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * @author https:www.unfbx.com
 * 2023-02-15
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeleteResponse implements Serializable {
    private String id;
    private String object;
    private boolean deleted;
}

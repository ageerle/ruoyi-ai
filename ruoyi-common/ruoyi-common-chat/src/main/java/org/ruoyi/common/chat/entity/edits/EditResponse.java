package org.ruoyi.common.chat.entity.edits;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.ruoyi.common.chat.entity.common.Choice;
import org.ruoyi.common.chat.entity.common.Usage;

import java.io.Serializable;

/**
 * 描述：
 *
 * @author https:www.unfbx.com
 *  2023-02-15
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EditResponse implements Serializable {
    private String id;
    private String object;
    private long created;
    private String model;
    private Choice[] choices;
    private Usage usage;
}

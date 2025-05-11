package org.ruoyi.common.chat.entity.billing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 *
 * @author https:www.unfbx.com
 * @since  2023-04-08
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Plan {
    private String title;
    private String id;
}

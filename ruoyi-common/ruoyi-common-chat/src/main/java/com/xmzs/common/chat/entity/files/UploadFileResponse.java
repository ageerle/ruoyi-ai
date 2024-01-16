package com.xmzs.common.chat.entity.files;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * 描述：
 *
 * @author https:www.unfbx.com
 *  2023-02-15
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadFileResponse extends File implements Serializable {
}

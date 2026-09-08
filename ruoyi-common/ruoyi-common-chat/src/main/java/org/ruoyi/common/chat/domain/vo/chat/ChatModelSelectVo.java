package org.ruoyi.common.chat.domain.vo.chat;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Public model option exposed to clients that can select a chat model.
 *
 * <p>This deliberately contains only the fields required to identify and display a model.
 * Operational configuration, including endpoints and credentials, remains server-side.</p>
 */
@Data
public class ChatModelSelectVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Model configuration identifier. */
    private Long id;

    /** Configured model name. */
    private String modelName;

    /** User-facing model description; clients can fall back to modelName when blank. */
    private String modelDescribe;

    /** Provider identifier used for display. */
    private String providerCode;

    public static ChatModelSelectVo from(ChatModelVo source) {
        ChatModelSelectVo target = new ChatModelSelectVo();
        target.setId(source.getId());
        target.setModelName(source.getModelName());
        target.setModelDescribe(source.getModelDescribe());
        target.setProviderCode(source.getProviderCode());
        return target;
    }
}

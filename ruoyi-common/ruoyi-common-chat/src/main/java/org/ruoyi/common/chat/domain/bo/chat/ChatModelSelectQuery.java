package org.ruoyi.common.chat.domain.bo.chat;

import lombok.Data;

/** Minimal, non-secret query contract for user-facing model selection. */
@Data
public class ChatModelSelectQuery {

    /** Optional model category; defaults to chat at the controller boundary. */
    private String category;
}

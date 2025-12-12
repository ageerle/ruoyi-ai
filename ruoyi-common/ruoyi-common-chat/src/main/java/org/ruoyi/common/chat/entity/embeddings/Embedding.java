package org.ruoyi.common.chat.entity.embeddings;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * @author https:www.unfbx.com
 * 2023-02-15
 */
@Getter
@Slf4j
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class Embedding implements Serializable {
    @NonNull
    @Builder.Default
    private String model = Model.TEXT_EMBEDDING_ADA_002.getName();
    /**
     * 必选项：长度不能超过：8192
     */
    @NonNull
    private List<String> input;

    private String user;

    public void setModel(Model model) {
        if (Objects.isNull(model)) {
            model = Model.TEXT_EMBEDDING_ADA_002;
        }
        this.model = model.getName();
    }


    public void setUser(String user) {
        this.user = user;
    }

    @Getter
    @AllArgsConstructor
    public enum Model {
        TEXT_EMBEDDING_ADA_002("text-embedding-ada-002"),
        ;
        private String name;
    }
}

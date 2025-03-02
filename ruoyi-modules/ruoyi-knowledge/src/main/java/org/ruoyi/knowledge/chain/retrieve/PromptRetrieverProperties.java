package org.ruoyi.knowledge.chain.retrieve;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class PromptRetrieverProperties {
    /**
     * 从知识库中检索的条数，limits 应大于 num
     */
    @Value("${chain.limits}")
    private int limits;

}

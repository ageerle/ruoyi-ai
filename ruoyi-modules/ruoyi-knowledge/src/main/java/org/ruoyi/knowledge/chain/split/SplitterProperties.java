package org.ruoyi.knowledge.chain.split;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "chain.split.chunk")
public class SplitterProperties {
    /**
     * 分段标识符
     */
    private String endspliter;

    /**
     * 提问分段标识符
     */
    private String qaspliter;

    /**
     * 分块文本大小
     */
    private int size;

    /**
     * 相邻块之间重叠的字符数(避免边界信息丢失)
     */
    private int overlay;
}

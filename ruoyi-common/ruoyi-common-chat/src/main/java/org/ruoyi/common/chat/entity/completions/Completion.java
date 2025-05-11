package org.ruoyi.common.chat.entity.completions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 *   问题类
 *
 * @author https:www.unfbx.com
 * 2023-02-11
 */
@Data
@Builder
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class Completion implements Serializable {

    @NonNull
    @Builder.Default
    private String model = Model.DAVINCI_003.getName();
    /**
     * 问题描述
     */
    @NonNull
    private String prompt;
    /**
     * 完成输出后的后缀，用于格式化输出结果
     */
    private String suffix;

    /**
     * 最大支持4096
     */
    @JsonProperty("max_tokens")
    @Builder.Default
    private Integer maxTokens = 2048;
    /**
     * 使用什么取样温度，0到2之间。较高的值(如0.8)将使输出更加随机，而较低的值(如0.2)将使输出更加集中和确定。
     * <p>
     * We generally recommend altering this or but not both.top_p
     */
    @Builder.Default
    private double temperature = 0;

    /**
     * 使用温度采样的替代方法称为核心采样，其中模型考虑具有top_p概率质量的令牌的结果。因此，0.1 意味着只考虑包含前 10% 概率质量的代币。
     * <p>
     * 我们通常建议更改此设置，但不要同时更改两者。temperature
     */
    @JsonProperty("top_p")
    @Builder.Default
    private Double topP = 1d;

    /**
     * 为每个提示生成的完成次数。
     */
    @Builder.Default
    private Integer n = 1;

    @Builder.Default
    private boolean stream = false;
    /**
     * 最大值：5
     */
    private Integer logprobs;

    @Builder.Default
    private boolean echo = false;

    private List<String> stop;

    @JsonProperty("presence_penalty")
    @Builder.Default
    private double presencePenalty = 0;

    /**
     * -2.0 ~~ 2.0
     */
    @JsonProperty("frequency_penalty")
    @Builder.Default
    private double frequencyPenalty = 0;

    @JsonProperty("best_of")
    @Builder.Default
    private Integer bestOf = 1;

    @JsonProperty("logit_bias")
    private Map logitBias;
    /**
     * 用户唯一值，确保接口不被重复调用
     */
    private String user;

    /**
     * 获取当前参数的tokens数
     * @return  token数量
     */
//    public long tokens() {
//        if (StrUtil.isBlank(this.prompt) || StrUtil.isBlank(this.model)) {
//            log.warn("参数异常model：{}，prompt：{}", this.model, this.prompt);
//            return 0;
//        }
//        return TikTokensUtil.tokens(this.model, this.prompt);
//    }

    @Getter
    @AllArgsConstructor
    public enum Model {
        DAVINCI_003("text-davinci-003"),
        DAVINCI_002("text-davinci-002"),
        DAVINCI("davinci"),
        ;
        private String name;
    }
}



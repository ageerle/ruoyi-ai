package org.ruoyi.common.chat.entity.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.ruoyi.common.chat.entity.chat.tool.Tools;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static org.ruoyi.common.chat.entity.chat.BaseChatCompletion.Model.GPT_3_5_TURBO;

/**
 * chat模型基础类
 *
 * @author https:www.unfbx.com
 * @since 1.1.2
 * 2023-11-10
 */
@Data
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class BaseChatCompletion implements Serializable {

    @NonNull
    @Builder.Default
    private String model = GPT_3_5_TURBO.getName();

    /**
     * 指定模型必须输出的格式的对象。
     *
     * @since 1.1.2
     */
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;

    /**
     * 已过时
     *
     * @see #tools
     */
    @Deprecated
    private List<Functions> functions;

    /**
     * 取值：null,auto或者自定义
     * functions没有值的时候默认为：null
     * functions存在值得时候默认为：auto
     * 也可以自定义
     * <p>已过时</p>
     *
     * @see #toolChoice
     */
    @Deprecated
    @JsonProperty("function_call")
    private Object functionCall;

    /**
     * 模型可能调用的工具列表。
     * 当前版本仅支持：functions
     *
     * @since 1.1.2
     */
    private List<Tools> tools;

    /**
     * 取值：String或者ToolChoiceObj
     *
     * @since 1.1.2
     */
    @JsonProperty("tool_choice")
    private Object toolChoice;

    /**
     * 使用什么取样温度，0到2之间。较高的值(如0.8)将使输出更加随机，而较低的值(如0.2)将使输出更加集中和确定。
     * <p>
     * We generally recommend altering this or but not both.top_p
     */
    @Builder.Default
    private double temperature = 0.2;

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


    /**
     * 是否流式输出.
     * default:false
     */
    @Builder.Default
    private boolean stream = false;
    /**
     * 停止输出标识
     */
    private List<String> stop;
    /**
     * 最大支持4096
     */
    @JsonProperty("max_tokens")
    @Builder.Default
    private Integer maxTokens = 2048;


    @JsonProperty("presence_penalty")
    @Builder.Default
    private double presencePenalty = 0;

    /**
     * -2.0 ~~ 2.0
     */
    @JsonProperty("frequency_penalty")
    @Builder.Default
    private double frequencyPenalty = 0;

    @JsonProperty("logit_bias")
    private Map logitBias;
    /**
     * 用户唯一值，确保接口不被重复调用
     */
    private String user;

    /**
     * @since 1.1.2
     */
    private Integer seed;


    /**
     * 最新模型参考官方文档：
     * <a href="https://platform.openai.com/docs/models/model-endpoint-compatibility">官方稳定模型列表</a>
     */
    @Getter
    @AllArgsConstructor
    public enum Model {
        /**
         * gpt-3.5-turbo
         */
        GPT_3_5_TURBO("gpt-3.5-turbo"),
        /**
         * 临时模型，不建议使用，2023年9 月 13 日将被弃用
         */
        @Deprecated
        GPT_3_5_TURBO_0301("gpt-3.5-turbo-0301"),
        /**
         * gpt-3.5-turbo-0613 支持函数
         */
        GPT_3_5_TURBO_1106("gpt-3.5-turbo-1106"),

        GPT_3_5_TURBO_0613("gpt-3.5-turbo-0613"),
        /**
         * gpt-3.5-turbo-16k 超长上下文
         */
        GPT_3_5_TURBO_16K("gpt-3.5-turbo-16k"),
        /**
         * gpt-3.5-turbo-16k-0613 超长上下文 支持函数
         */
        GPT_3_5_TURBO_16K_0613("gpt-3.5-turbo-16k-0613"),
        /**
         * gpt-3.5-turbo-0125 超长上下文 支持函数
         */
        GPT_3_5_TURBO_0125("gpt-3.5-turbo-0125"),
        /**
         * GPT4.0
         */
        GPT_4("gpt-4"),
        /**
         * 临时模型，不建议使用，2023年9 月 13 日将被弃用
         */
        @Deprecated
        GPT_4_0314("gpt-4-0314"),
        /**
         * GPT4.0 超长上下文
         */
        GPT_4_32K("gpt-4-32k"),
        /**
         * 临时模型，不建议使用，2023年9 月 13 日将被弃用
         */
        @Deprecated
        GPT_4_32K_0314("gpt-4-32k-0314"),

        /**
         * gpt-4-0613，支持函数
         */
        GPT_4_0613("gpt-4-0613"),
        /**
         * gpt-4-0613，支持函数
         */
        GPT_4_32K_0613("gpt-4-32k-0613"),
        /**
         * 支持数组模式，支持function call，支持可重复输出
         */
        GPT_4_1106_PREVIEW("gpt-4-1106-preview"),
        /**
         * 支持图片
         */
        GPT_4_VISION_PREVIEW("gpt-4-vision-preview"),
        /**
         * gpt-4-0613，支持函数
         */
        GPT_4_0125_PREVIEW("gpt-4-0125-preview"),

        /**
         * GPT_4_ALL
         */
        GPT_4_ALL("gpt-4-all"),

        GPT_4_GIZMO("gpt-4-gizmo"),

        NET("net"),

        CLAUDE_3_SONNET("claude-3-sonnet-20240229"),

        GEMINI_PRO("gemini-pro"),

        STABLE_DIFFUSION("stable-diffusion"),

        SUNO_V3("suno-v3"),
        ;
        private final String name;
    }

    @Getter
    @AllArgsConstructor
    public enum ChatType {
        /**
         * 对话类型 - 输入
         */
        CHAT_IN("in"),
        /**
         * 对话类型 - 输出
         */
        CHAT_OUT("out"),

        ;
        private final String name;
    }

}

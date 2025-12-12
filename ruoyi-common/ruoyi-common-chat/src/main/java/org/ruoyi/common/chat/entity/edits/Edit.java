package org.ruoyi.common.chat.entity.edits;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * @author https:www.unfbx.com
 * 2023-02-15
 */
@Getter
@Builder
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class Edit implements Serializable {
    /**
     * 编辑模型，目前支持两种
     */
    @NonNull
    private String model;

    @NonNull
    private String input;
    /**
     * 提示说明。告知模型如何修改。
     */
    @NonNull
    private String instruction;


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

    public void setModel(Model model) {
        this.model = model.getName();
    }

    public void setTemperature(double temperature) {
        if (temperature > 2 || temperature < 0) {
            log.error("temperature参数异常，temperature属于[0,2]");
            this.temperature = 2;
            return;
        }
        if (temperature < 0) {
            log.error("temperature参数异常，temperature属于[0,2]");
            this.temperature = 0;
            return;
        }
        this.temperature = temperature;
    }


    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public void setN(Integer n) {
        this.n = n;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    @Getter
    @AllArgsConstructor
    public enum Model {
        TEXT_DAVINCI_EDIT_001("text-davinci-edit-001"),
        CODE_DAVINCI_EDIT_001("code-davinci-edit-001"),
        ;
        private String name;
    }
}




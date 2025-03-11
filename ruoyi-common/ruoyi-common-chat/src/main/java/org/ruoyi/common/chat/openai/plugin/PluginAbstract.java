package org.ruoyi.common.chat.openai.plugin;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.ruoyi.common.chat.entity.chat.Parameters;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public abstract class PluginAbstract<R extends PluginParam, T> {

    private Class<?> R;

    private String name;

    private String function;

    private String description;

    private List<Arg> args;

    private List<String> required;

    private Parameters parameters;

    public PluginAbstract(Class<?> r) {
        R = r;
    }

    public void setRequired(List<String> required) {
        if (CollectionUtil.isEmpty(required)) {
            this.required = this.getArgs().stream().filter(e -> e.isRequired()).map(Arg::getName).collect(Collectors.toList());
            return;
        }
        this.required = required;
    }

    private void setRequired() {
        if (CollectionUtil.isEmpty(required)) {
            this.required = this.getArgs().stream().filter(e -> e.isRequired()).map(Arg::getName).collect(Collectors.toList());
        }
    }

    private void setParameters() {
        JSONObject properties = new JSONObject();
        args.forEach(e -> {
            JSONObject param = new JSONObject();
            param.putOpt("type", e.getType());
            param.putOpt("enum", e.getEnumDictValue());
            param.putOpt("description", e.getDescription());
            properties.putOpt(e.getName(), param);
        });
        this.parameters = Parameters.builder()
                .type("object")
                .properties(properties)
                .required(this.getRequired())
                .build();
    }

    public void setArgs(List<Arg> args) {
        this.args = args;
        setRequired();
        setParameters();
    }

    @Data
    public static class Arg {
        private String name;
        private String type;
        private String description;
        @JsonIgnore
        private boolean enumDict;
        @JsonProperty("enum")
        private List<String> enumDictValue;
        @JsonIgnore
        private boolean required;
    }

    public abstract T func(R args);

    public abstract String content(T t);
}

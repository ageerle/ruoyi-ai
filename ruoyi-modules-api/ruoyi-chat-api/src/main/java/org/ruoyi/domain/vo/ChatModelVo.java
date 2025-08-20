package org.ruoyi.domain.vo;


import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.Getter;
import org.ruoyi.common.sensitive.annotation.Sensitive;
import org.ruoyi.common.sensitive.core.SensitiveStrategy;
import org.ruoyi.domain.ChatModel;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;


/**
 * 聊天模型视图对象 chat_model
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ChatModel.class)
public class ChatModelVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 模型分类
     */
    @ExcelProperty(value = "模型分类")
    private String category;

    /**
     * 模型名称
     */
    @ExcelProperty(value = "模型名称")
    private String modelName;

    /**
     * 模型描述
     */
    @ExcelProperty(value = "模型描述")
    private String modelDescribe;

    /**
     * 模型价格
     */
    @ExcelProperty(value = "模型价格")
    private Double modelPrice;

    /**
     * 计费类型
     */
    @ExcelProperty(value = "计费类型")
    private String modelType;

    /**
     * 是否显示
     */
    @ExcelProperty(value = "是否显示")
    private String modelShow;

    /**
     * 系统提示词
     */
    @ExcelProperty(value = "系统提示词")
    private String systemPrompt;

    /**
     * 请求地址
     */
    @ExcelProperty(value = "请求地址")
    private String apiHost;

    /**
     * 密钥
     */
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    @ExcelProperty(value = "密钥")
    private String apiKey;

    /**
     * 优先级(值越大优先级越高)
     */
    @ExcelProperty(value = "优先级")
    private Integer priority;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 模型能力
     */
    @ExcelProperty(value = "模型能力")
    private String modelCapability;

    /**
     * 模型能力列表
     */
    private List<Ability> modelAbilities = getModelAbilities();

    /**
     * 模型能力类，类似枚举的静态内部类
     */
    @Getter
    public static final class Ability {
        // 获取能力名称
        private final String name;
        private final String description;

        // 静态字段存储默认能力（类似枚举常量）
        public static final Ability IMAGE = new Ability("IMAGE", "图片理解");
        public static final Ability VIDEO = new Ability("VIDEO", "视频理解");
        public static final Ability SPEECH = new Ability("SPEECH", "语音理解");

        // 动态扩展能力存储
        private static final java.util.Map<String, Ability> EXTENDED_ABILITIES = new java.util.HashMap<>();

        // 私有构造确保受限
        private Ability(String name, String description) {
            this.name = name;
            this.description = description;
        }

        // 静态工厂方法（类似枚举valueOf）
        public static Ability valueOf(String name) {
            // 先检查默认能力
            switch (name) {
                case "IMAGE": return IMAGE;
                case "VIDEO": return VIDEO;
                case "SPEECH": return SPEECH;
                default:
                    // 检查扩展能力
                    Ability ability = EXTENDED_ABILITIES.get(name);
                    if (ability != null) return ability;
                    throw new IllegalArgumentException("Unknown ability: " + name);
            }
        }

        // 动态注册新能力（后期从数据库调用）
        public static synchronized Ability registerAbility(String name, String description) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Ability name cannot be empty");
            }

            // 避免重复注册
            if (EXTENDED_ABILITIES.containsKey(name)) {
                return EXTENDED_ABILITIES.get(name);
            }

            // 检查是否与默认能力冲突
            try {
                valueOf(name);
                throw new IllegalArgumentException("Ability already exists as default: " + name);
            } catch (IllegalArgumentException e) {
                // 正常情况，继续注册
            }

            Ability newAbility = new Ability(name, description);
            EXTENDED_ABILITIES.put(name, newAbility);
            return newAbility;
        }

        // 获取所有能力（默认+扩展）
        public static java.util.Set<Ability> getAllAbilities() {
            java.util.Map<String, Ability> all = new java.util.HashMap<>();
            all.put(IMAGE.name, IMAGE);
            all.put(VIDEO.name, VIDEO);
            all.put(SPEECH.name, SPEECH);
            all.putAll(EXTENDED_ABILITIES);
            return java.util.Collections.unmodifiableSet((java.util.Set<? extends Ability>) all.values());
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Ability ability = (Ability) o;
            return name.equals(ability.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    /**
     * 将 modelCapability 字符串转换为 Ability 列表
     * @return Ability 列表
     */
    public java.util.List<Ability> getModelAbilities() {
        if (modelCapability == null || modelCapability.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // 解析 JSON 格式的字符串数组
        String trimmed = modelCapability.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("Invalid modelCapability format: " + modelCapability);
        }

        String content = trimmed.substring(1, trimmed.length() - 1).trim();
        if (content.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        java.util.List<Ability> abilities = new java.util.ArrayList<>();
        String[] items = content.split(",");
        for (String item : items) {
            String cleanedItem = item.trim();
            if (cleanedItem.startsWith("\"") && cleanedItem.endsWith("\"") && cleanedItem.length() >= 2) {
                String abilityName = cleanedItem.substring(1, cleanedItem.length() - 1);
                abilities.add(Ability.valueOf(abilityName));
            }
        }

        return abilities;
    }
}
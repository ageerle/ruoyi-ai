package org.ruoyi.common.json.handler;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.ruoyi.common.core.utils.ObjectUtils;

import java.io.IOException;
import java.util.Date;

/**
 * 自定义 Date 类型反序列化处理器（支持多种格式）
 *
 * @author AprilWind
 */
public class CustomDateDeserializer extends JsonDeserializer<Date> {

    /**
     * 反序列化逻辑：将字符串转换为 Date 对象
     *
     * @param p    JSON 解析器，用于获取字符串值
     * @param ctxt 上下文环境（可用于获取更多配置）
     * @return 转换后的 Date 对象，若为空字符串返回 null
     * @throws IOException 当字符串格式非法或转换失败时抛出
     */
    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        DateTime parse = DateUtil.parse(p.getText());
        if (ObjectUtils.isNull(parse)) {
            return null;
        }
        return parse.toJdkDate();
    }

}

package org.ruoyi.system.listener;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;

public class CustomerBigDecimalSerialize extends JsonSerializer<BigDecimal> {
    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if(Objects.nonNull(value)) {
            //返回到前端的数据为数字类型,前端接收有可能丢失精度
            //gen.writeNumber(value.stripTrailingZeros());
            //返回到前端的数据为字符串类型
            gen.writeString(value.stripTrailingZeros().toPlainString());
            //去除0后缀,如果想统一进行保留精度，也可以采用类似处理
        }else {//如果为null的话，就写null
            gen.writeNull();
        }
    }
}


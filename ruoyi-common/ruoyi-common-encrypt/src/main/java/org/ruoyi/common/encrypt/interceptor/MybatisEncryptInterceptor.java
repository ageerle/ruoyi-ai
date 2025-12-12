package org.ruoyi.common.encrypt.interceptor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.encrypt.annotation.EncryptField;
import org.ruoyi.common.encrypt.core.EncryptContext;
import org.ruoyi.common.encrypt.core.EncryptorManager;
import org.ruoyi.common.encrypt.enumd.AlgorithmType;
import org.ruoyi.common.encrypt.enumd.EncodeType;
import org.ruoyi.common.encrypt.properties.EncryptorProperties;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * 入参加密拦截器
 *
 * @author 老马
 * @version 4.6.0
 */
@Slf4j
@Intercepts({@Signature(
        type = ParameterHandler.class,
        method = "setParameters",
        args = {PreparedStatement.class})
})
@AllArgsConstructor
public class MybatisEncryptInterceptor implements Interceptor {

    private final EncryptorManager encryptorManager;
    private final EncryptorProperties defaultProperties;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        return invocation;
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof ParameterHandler parameterHandler) {
            // 进行加密操作
            Object parameterObject = parameterHandler.getParameterObject();
            if (ObjectUtil.isNotNull(parameterObject) && !(parameterObject instanceof String)) {
                this.encryptHandler(parameterObject);
            }
        }
        return target;
    }

    /**
     * 加密对象
     *
     * @param sourceObject 待加密对象
     */
    private void encryptHandler(Object sourceObject) {
        if (ObjectUtil.isNull(sourceObject)) {
            return;
        }
        if (sourceObject instanceof Map<?, ?> map) {
            new HashSet<>(map.values()).forEach(this::encryptHandler);
            return;
        }
        if (sourceObject instanceof List<?> list) {
            if (CollUtil.isEmpty(list)) {
                return;
            }
            // 判断第一个元素是否含有注解。如果没有直接返回，提高效率
            Object firstItem = list.get(0);
            if (ObjectUtil.isNull(firstItem) || CollUtil.isEmpty(encryptorManager.getFieldCache(firstItem.getClass()))) {
                return;
            }
            list.forEach(this::encryptHandler);
            return;
        }
        Set<Field> fields = encryptorManager.getFieldCache(sourceObject.getClass());
        try {
            for (Field field : fields) {
                field.set(sourceObject, this.encryptField(String.valueOf(field.get(sourceObject)), field));
            }
        } catch (Exception e) {
            log.error("处理加密字段时出错", e);
        }
    }

    /**
     * 字段值进行加密。通过字段的批注注册新的加密算法
     *
     * @param value 待加密的值
     * @param field 待加密字段
     * @return 加密后结果
     */
    private String encryptField(String value, Field field) {
        if (ObjectUtil.isNull(value)) {
            return null;
        }
        EncryptField encryptField = field.getAnnotation(EncryptField.class);
        EncryptContext encryptContext = new EncryptContext();
        encryptContext.setAlgorithm(encryptField.algorithm() == AlgorithmType.DEFAULT ? defaultProperties.getAlgorithm() : encryptField.algorithm());
        encryptContext.setEncode(encryptField.encode() == EncodeType.DEFAULT ? defaultProperties.getEncode() : encryptField.encode());
        encryptContext.setPassword(StringUtils.isBlank(encryptField.password()) ? defaultProperties.getPassword() : encryptField.password());
        encryptContext.setPrivateKey(StringUtils.isBlank(encryptField.privateKey()) ? defaultProperties.getPrivateKey() : encryptField.privateKey());
        encryptContext.setPublicKey(StringUtils.isBlank(encryptField.publicKey()) ? defaultProperties.getPublicKey() : encryptField.publicKey());
        return this.encryptorManager.encrypt(value, encryptContext);
    }


    @Override
    public void setProperties(Properties properties) {
    }
}

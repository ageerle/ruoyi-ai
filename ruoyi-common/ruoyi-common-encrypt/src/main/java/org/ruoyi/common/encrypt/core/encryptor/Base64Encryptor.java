package org.ruoyi.common.encrypt.core.encryptor;

import cn.hutool.core.codec.Base64;
import org.ruoyi.common.encrypt.core.EncryptContext;
import org.ruoyi.common.encrypt.enumd.AlgorithmType;
import org.ruoyi.common.encrypt.enumd.EncodeType;

/**
 * Base64算法实现
 *
 * @author 老马
 * @version 4.6.0
 */
public class Base64Encryptor extends AbstractEncryptor {

    public Base64Encryptor(EncryptContext context) {
        super(context);
    }

    /**
     * 获得当前算法
     */
    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.BASE64;
    }

    /**
     * 加密
     *
     * @param value      待加密字符串
     * @param encodeType 加密后的编码格式
     */
    @Override
    public String encrypt(String value, EncodeType encodeType) {
        return Base64.encode(value);
    }

    /**
     * 解密
     *
     * @param value      待加密字符串
     */
    @Override
    public String decrypt(String value) {
        return Base64.decodeStr(value);
    }
}

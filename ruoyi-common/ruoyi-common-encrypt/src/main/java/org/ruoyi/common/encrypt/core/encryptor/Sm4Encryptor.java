package org.ruoyi.common.encrypt.core.encryptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SM4;
import org.ruoyi.common.encrypt.core.EncryptContext;
import org.ruoyi.common.encrypt.enumd.AlgorithmType;
import org.ruoyi.common.encrypt.enumd.EncodeType;

import java.nio.charset.StandardCharsets;

/**
 * sm4算法实现
 *
 * @author 老马
 * @version 4.6.0
 */
public class Sm4Encryptor extends AbstractEncryptor {

    private final SM4 sm4;

    public Sm4Encryptor(EncryptContext context) {
        super(context);
        String password = context.getPassword();
        if (StrUtil.isBlank(password)) {
            throw new IllegalArgumentException("SM4没有获得秘钥信息");
        }
        // sm4算法的秘钥要求是16位长度
        if (16 != password.length()) {
            throw new IllegalArgumentException("SM4秘钥长度应该为16位，实际为" + password.length() + "位");
        }
        this.sm4 = SmUtil.sm4(password.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 获得当前算法
     */
    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.SM4;
    }

    /**
     * 加密
     *
     * @param value      待加密字符串
     * @param encodeType 加密后的编码格式
     */
    @Override
    public String encrypt(String value, EncodeType encodeType) {
        if (encodeType == EncodeType.HEX) {
            return sm4.encryptHex(value);
        } else {
            return sm4.encryptBase64(value);
        }
    }

    /**
     * 解密
     *
     * @param value      待加密字符串
     */
    @Override
    public String decrypt(String value) {
        return this.sm4.decryptStr(value);
    }
}

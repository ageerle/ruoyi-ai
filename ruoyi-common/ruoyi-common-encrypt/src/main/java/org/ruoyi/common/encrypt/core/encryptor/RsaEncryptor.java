package org.ruoyi.common.encrypt.core.encryptor;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.encrypt.core.EncryptContext;
import org.ruoyi.common.encrypt.enumd.AlgorithmType;
import org.ruoyi.common.encrypt.enumd.EncodeType;


/**
 * RSA算法实现
 *
 * @author 老马
 * @version 4.6.0
 */
public class RsaEncryptor extends AbstractEncryptor {

    private final RSA rsa;

    public RsaEncryptor(EncryptContext context) {
        super(context);
        String privateKey = context.getPrivateKey();
        String publicKey = context.getPublicKey();
        if (StringUtils.isAnyEmpty(privateKey, publicKey)) {
            throw new IllegalArgumentException("RSA公私钥均需要提供，公钥加密，私钥解密。");
        }
        this.rsa = SecureUtil.rsa(Base64.decode(privateKey), Base64.decode(publicKey));
    }

    /**
     * 获得当前算法
     */
    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.RSA;
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
            return rsa.encryptHex(value, KeyType.PublicKey);
        } else {
            return rsa.encryptBase64(value, KeyType.PublicKey);
        }
    }

    /**
     * 解密
     *
     * @param value      待加密字符串
     */
    @Override
    public String decrypt(String value) {
        return this.rsa.decryptStr(value, KeyType.PrivateKey);
    }
}

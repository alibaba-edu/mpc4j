package edu.alibaba.mpc4j.common.tool.crypto.kdf;

import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory.KdfType;

/**
 * 密钥派生函数（Key Derivation Function，KDF）接口。
 *
 * @author Weiran Liu
 * @date 2021/12/31
 */
public interface Kdf {

    /**
     * 将输入种子派生为密钥。
     *
     * @param seed 输入种子。
     * @return 密钥。
     */
    byte[] deriveKey(byte[] seed);

    /**
     * 返回密钥派生函数类型。
     *
     * @return 密钥派生函数类型。
     */
    KdfType getKdfType();
}

package edu.alibaba.mpc4j.crypto.phe.params;

/**
 * 半同态加密私钥。
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
public interface PhePrivateKey extends PheKeyParams {
    /**
     * 返回公钥。
     *
     * @return 公钥。
     */
    PhePublicKey getPublicKey();
}

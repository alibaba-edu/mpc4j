package edu.alibaba.mpc4j.common.tool.crypto.prp;

import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;

/**
 * 伪随机置换（Pseudo-Random Permutation，PRP）接口。PRP使用{0,1}^κ的密钥进行初始化，以{0,1}^κ为输入，返回{0,1}^κ的输出。
 *
 * @author Weiran Liu
 * @date 2021/11/30
 */
public interface Prp {
    /**
     * 设置密钥。密钥将被拷贝，防止后续可能的篡改。
     *
     * @param key 密钥。
     */
    void setKey(byte[] key);

    /**
     * 对明文伪随机置换。
     *
     * @param plaintext κ比特长明文。
     * @return κ比特长密文。
     */
    byte[] prp(byte[] plaintext);

    /**
     * 对密文逆伪随机置换。
     *
     * @param ciphertext κ比特长密文。
     * @return κ比特长明文。
     */
    byte[] invPrp(byte[] ciphertext);

    /**
     * 返回伪随机置换类型。
     *
     * @return 伪随机置换类型。
     */
    PrpType getPrpType();
}

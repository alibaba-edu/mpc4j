package edu.alibaba.mpc4j.common.tool.okve.ovdm.zp;

import edu.alibaba.mpc4j.common.tool.okve.ovdm.zp.ZpOvdmFactory.ZpOvdmType;

import java.math.BigInteger;
import java.util.Map;

/**
 * Zp域不经意映射值解密匹配（Oblivious Value Decryption Matching，OVDM）。
 *
 * @author Weiran Liu
 * @date 2021/10/01
 */
public interface ZpOvdm<T> {

    /**
     * 编码键值对。
     *
     * @param keyValueMap 键值对。
     * @return 不经意键值存储器。
     * @throws ArithmeticException 如果无法完成编码。
     */
    BigInteger[] encode(Map<T, BigInteger> keyValueMap) throws ArithmeticException;

    /**
     * 返回键值的映射值。
     *
     * @param storage 不经意键值存储器。
     * @param key     键值。
     * @return 映射值。
     */
    BigInteger decode(BigInteger[] storage, T key);

    /**
     * 返回OVDM允许编码的键值对数量。
     *
     * @return 允许编码的键值对数量。
     */
    int getN();

    /**
     * 返回OVDM的行数，满足{@code m / Byte.SIZE == 0}。
     *
     * @return 行数。
     */
    int getM();

    /**
     * 返回OVDM的编码比率，即键值对数量（{@code n}）和OKVS的行数（{@code m}）。编码比率越大（越接近于1），说明OVDM的编码压缩率越高。
     *
     * @return 编码比率。
     */
    default double rate() {
        return ((double)getN()) / getM();
    }

    /**
     * 返回Zp-OVDM类型。
     *
     * @return Zp-OVDM类型。
     */
    ZpOvdmType getZpOvdmType();

    /**
     * 返回OVDM的编码失败概率。假定失败概率为p，则返回-log_2(p)。
     *
     * @return OVDM的编码失败概率。
     */
    int getNegLogFailureProbability();
}

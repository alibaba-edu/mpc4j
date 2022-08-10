package edu.alibaba.mpc4j.common.tool.okve.ovdm.gf2e;

import java.util.Map;

/**
 * GF(2^l)不经意映射值解密匹配（Oblivious Value Decryption Matching，OVDM）。最开始的定义来自于：
 * Rindal P, Schoppmann P. VOLE-PSI: Fast OPRF and Circuit-PSI from Vector-OLE. EUROCRYPT 2021.
 * Springer, Cham, pp. 901-930.
 *
 * @author Weiran Liu
 * @date 2021/09/27
 */
public interface Gf2eOvdm<T> {

    /**
     * 返回GF(2^l)-OVDM类型。
     *
     * @return GF(2^l)-OVDM类型。
     */
    Gf2eOvdmFactory.Gf2eOvdmType getGf2xOvdmType();

    /**
     * 编码键值对。
     *
     * @param keyValueMap 键值对。
     * @return 不经意键值存储器。
     * @throws ArithmeticException 如果无法完成编码。
     */
    byte[][] encode(Map<T, byte[]> keyValueMap) throws ArithmeticException;

    /**
     * 返回键值的映射值。
     *
     * @param storage 不经意键值存储器。
     * @param key     键值。
     * @return 映射值。
     */
    byte[] decode(byte[][] storage, T key);

    /**
     * 返回OVDM允许编码的键值对数量。
     *
     * @return 允许编码的键值对数量。
     */
    int getN();

    /**
     * 返回OVDM映射值的最大比特长度，满足{@code l % Byte.SIZE == 0}。
     *
     * @return 映射值的最大比特长度。
     */
    int getL();

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
        return ((double)this.getN()) / this.getM();
    }

    /**
     * 返回OVDM的编码失败概率。假定失败概率为p，则返回-log_2(p)。
     *
     * @return OVDM的编码失败概率。
     */
    int getNegLogFailureProbability();
}

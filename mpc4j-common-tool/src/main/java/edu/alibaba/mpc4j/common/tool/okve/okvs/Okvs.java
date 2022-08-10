package edu.alibaba.mpc4j.common.tool.okve.okvs;

import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;

import java.util.Map;

/**
 * 不经意键值存储（Oblivious Key-Value Storage，OKVS）。
 *
 * OKVS的定义来自于：
 * Garimella G, Pinkas B, Rosulek M, et al. Oblivious Key-Value Stores and Amplification for Private Set Intersection.
 * CRYPTO 2021, Springer, Cham, 2021, pp. 395-425.
 *
 * @author Weiran Liu
 * @date 2021/09/05
 */
public interface Okvs<T> {
    /**
     * 设置是否并发编码。
     *
     * @param parallelEncode 是否并发编码。
     */
    void setParallelEncode(boolean parallelEncode);

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
     * 返回OKVS允许编码的键值对数量。
     *
     * @return 允许编码的键值对数量。
     */
    int getN();

    /**
     * 返回OKVS映射值的最大比特长度，满足{@code l % Byte.SIZE == 0}。
     *
     * @return 映射值的最大比特长度。
     */
    int getL();

    /**
     * 返回OKVS的行数，满足{@code m / Byte.SIZE == 0}。
     *
     * @return 行数。
     */
    int getM();

    /**
     * 返回OKVS的编码比率，即键值对数量（{@code n}）和OKVS的行数（{@code m}）。编码比率越大（越接近于1），说明OKVS的编码压缩率越高。
     *
     * @return 编码比率。
     */
    default double rate() {
        return ((double)this.getN()) / this.getM();
    }

    /**
     * 返回OKVS类型。
     *
     * @return OKVS类型。
     */
    OkvsType getOkvsType();

    /**
     * 返回OKVS的编码失败概率。假定失败概率为p，则返回-log_2(p)。
     *
     * @return OKVS的编码失败概率。
     */
    int getNegLogFailureProbability();
}

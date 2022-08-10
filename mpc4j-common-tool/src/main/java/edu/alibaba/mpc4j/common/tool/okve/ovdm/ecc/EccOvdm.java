package edu.alibaba.mpc4j.common.tool.okve.ovdm.ecc;

import org.bouncycastle.math.ec.ECPoint;

import java.util.Map;

/**
 * 椭圆曲线不经意映射值解密匹配（Oblivious Value Decryption Matching，OVDM）。最开始的定义来自于：
 * Rindal P, Schoppmann P. VOLE-PSI: Fast OPRF and Circuit-PSI from Vector-OLE. EUROCRYPT 2021.
 * Springer, Cham, pp. 901-930.
 *
 * 后续论文发现此技术可以进一步扩展，支持隐私集合求并（Private Set Union），论文来源：
 * Cong Z, Yu C, Weiran L, et al. Optimal Private Set Union from Batch Reverse Private Membership Test. Submitted to
 * EUROCRYPT 2022, under review.
 *
 * @author Weiran Liu
 * @date 2021/09/07
 */
public interface EccOvdm<T> {
    /**
     * 编码键值对。
     *
     * @param keyValueMap 键值对。
     * @return 不经意键值存储器。
     * @throws ArithmeticException 如果无法完成编码。
     */
    ECPoint[] encode(Map<T, ECPoint> keyValueMap) throws ArithmeticException;

    /**
     * 返回键值的映射值。
     *
     * @param storage 不经意键值存储器。
     * @param key     键值。
     * @return 映射值。
     */
    ECPoint decode(ECPoint[] storage, T key);

    /**
     * 返回OVDM允许编码的键值对数量。
     *
     * @return 允许编码的键值对数量。
     */
    int getN();

    /**
     * 返回OKVS的行数，满足{@code m / Byte.SIZE == 0}。
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
     * 返回椭圆曲线OVDM类型。
     *
     * @return 椭圆曲线OVDM类型。
     */
    EccOvdmFactory.EccOvdmType getEccOvdmType();

    /**
     * 返回OVDM的编码失败概率。假定失败概率为p，则返回-log_2(p)。
     *
     * @return OVDM的编码失败概率。
     */
    int getNegLogFailureProbability();
}

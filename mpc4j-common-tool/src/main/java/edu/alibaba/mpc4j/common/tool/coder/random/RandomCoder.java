package edu.alibaba.mpc4j.common.tool.coder.random;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.coder.Coder;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * 伪随机编码（Pseudo-Random Coder，PRC）。在指定的调用次数下，PRC可以保证数据结果的最小汉明距离满足要求。PRC由下述论文提出：
 * Kolesnikov V, Kumaresan R, Rosulek M, et al. Efficient batched oblivious PRF with applications to private set
 * intersection. CCS 2016, ACM, 2016, pp. 818-829.
 *
 * @author Weiran Liu
 * @date 2021/12/20
 */
public class RandomCoder implements Coder {
    /**
     * 码字字节长度
     */
    private final int codewordByteLength;
    /**
     * 码字比特长度
     */
    private final int codewordBitLength;
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * 伪随机函数
     */
    private final Prf prf;

    /**
     * 构造伪随机编码。
     *
     * @param envType 环境类型。
     * @param codewordByteLength 码字字节长度。
     */
    public RandomCoder(EnvType envType, int codewordByteLength) {
        assert codewordByteLength >= RandomCoderUtils.getMinCodewordByteLength()
            && codewordByteLength <= RandomCoderUtils.getMaxCodewordByteLength();
        this.envType = envType;
        this.codewordByteLength = codewordByteLength;
        codewordBitLength = codewordByteLength * Byte.SIZE;
        prf = PrfFactory.createInstance(envType, codewordByteLength);
    }

    /**
     * 设置密钥，密钥将被拷贝，以防止后续可能发生的修改。
     *
     * @param key 密钥。
     */
    public void setKey(byte[] key) {
        prf.setKey(key);
    }

    /**
     * 返回密钥。
     *
     * @return 密钥。
     */
    public byte[] getKey() {
        return prf.getKey();
    }

    @Override
    public int getDatawordBitLength() {
        // 设置为Integer.MAX_VALUE - Byte.SIZE，保证+1的值 > 0，且保证转换成ByteLength后再乘以Byte.SIZE不会小于0。
        return Integer.MAX_VALUE - Byte.SIZE;
    }

    @Override
    public int getDatawordByteLength() {
        return CommonUtils.getByteLength(Integer.MAX_VALUE - Byte.SIZE);
    }

    @Override
    public int getCodewordBitLength() {
        return codewordBitLength;
    }

    @Override
    public int getCodewordByteLength() {
        return codewordByteLength;
    }

    @Override
    public int getMinimalHammingDistance() {
        // 伪随机编码要求最小汉明距离有极高的概率大于等于计算安全常数
        return CommonConstants.BLOCK_BIT_LENGTH;
    }

    @Override
    public byte[] encode(byte[] input) {
        return prf.getBytes(input);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RandomCoder)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        RandomCoder that = (RandomCoder)obj;
        return new EqualsBuilder()
            .append(this.codewordByteLength, that.codewordByteLength)
            .append(this.envType, that.envType)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(codewordBitLength)
            .append(envType)
            .toHashCode();
    }
}

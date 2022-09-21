package edu.alibaba.mpc4j.common.tool.crypto.kyber.params;

/**
 * Helper class for random uniform matrix usage. Modified from:
 * <p>
 * https://github.com/fisherstevenk/kyberJCE/blob/main/src/main/java/com/swiftcryptollc/crypto/provider/KyberUniformRandom.java
 * </p>
 * The modification is for removing unnecessary import packages.
 *
 * @author Steven K Fisher, Sheng Hu, Weiran Liu
 */
public class KyberUniformRandom {
    /**
     * 随机数
     */
    private short[] uniformRandom;
    /**
     * 随机数区间
     */
    private int uniformIntervalBound = 0;

    /**
     * 构造Kyber均匀随机数。
     */
    public KyberUniformRandom() {
        // empty
    }

    /**
     * 返回均匀随机数。
     *
     * @return 均匀随机数。
     */
    public short[] getUniformRandom() {
        return uniformRandom;
    }

    /**
     * 设置均匀随机数。
     *
     * @param uniformRandom 均匀随机数。
     */
    public void setUniformRandom(short[] uniformRandom) {
        this.uniformRandom = uniformRandom;
    }

    /**
     * 返回均匀随机数间隔上界。
     *
     * @return 均匀随机数间隔上界。
     */
    public int getUniformIntervalBound() {
        return uniformIntervalBound;
    }

    /**
     * 设置均匀随机数间隔上界。
     *
     * @param uniformIntervalBound 均匀随机数间隔上界。
     */
    public void setUniformIntervalBound(int uniformIntervalBound) {
        this.uniformIntervalBound = uniformIntervalBound;
    }
}

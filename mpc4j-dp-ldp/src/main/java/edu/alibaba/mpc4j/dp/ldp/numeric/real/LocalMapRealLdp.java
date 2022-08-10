package edu.alibaba.mpc4j.dp.ldp.numeric.real;

import edu.alibaba.mpc4j.common.sampler.real.laplace.ApacheLaplaceSampler;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import org.apache.commons.math3.util.Precision;

/**
 * 本地映射实数LDP机制。
 *
 * @author Weiran Liu
 * @date 2022/5/3
 */
class LocalMapRealLdp implements RealLdp {
    /**
     * 默认重采样次数
     */
    private static final int MAX_RESAMPLE = 1 << 20;
    /**
     * 配置项
     */
    private LocalMapRealLdpConfig localMapRealLdpConfig;
    /**
     * 分区长度θ
     */
    private double theta;
    /**
     * Apache拉普拉斯分布采样器
     */
    private ApacheLaplaceSampler apacheLaplaceSampler;

    @Override
    public void setup(LdpConfig ldpConfig) {
        assert ldpConfig instanceof LocalMapRealLdpConfig;
        localMapRealLdpConfig = (LocalMapRealLdpConfig) ldpConfig;
        // 设置分区长度θ
        theta = localMapRealLdpConfig.getTheta();
        // 计算放缩系数b = 2 / ε
        double b = 2.0 / localMapRealLdpConfig.getBaseEpsilon();
        // 设置Laplace采样器
        apacheLaplaceSampler = new ApacheLaplaceSampler(localMapRealLdpConfig.getRandomGenerator(), 0.0, b);
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        localMapRealLdpConfig.getRandomGenerator().setSeed(seed);
    }

    @Override
    public String getMechanismName() {
        return "[" + getLowerBound() + ", " + getUpperBound() + "], " +
            "(θ = " + theta + ", ε = " + localMapRealLdpConfig.getBaseEpsilon() + ")-" +
            getClass().getSimpleName();
    }

    @Override
    public LdpConfig getLdpConfig() {
        return localMapRealLdpConfig;
    }

    @Override
    public double randomize(double value) {
        double lowerBound = localMapRealLdpConfig.getLowerBound();
        double upperBound = localMapRealLdpConfig.getUpperBound();
        assert value >= lowerBound && value <= upperBound : "value must be in range [" + lowerBound + ", " + upperBound + "]";
        // 计算分区内的值：如果真实值大于0，正常处理；如果真实值小于0，则取模后也小于0，需要再往右移动一个θ
        double partitionValue = getPartitionValue(value);
        int partitionIndex = getPartitionIndex(value);
        // 在partitionValue上应用敏感度等于1的拉普拉斯机制，并验证结果是否在[0, θ)间，如果不满足，则重采样
        int count = 0;
        while (count <= MAX_RESAMPLE) {
            count++;
            double noiseValue = apacheLaplaceSampler.sample() + partitionValue;
            if (noiseValue >= 0 && noiseValue < theta) {
                return partitionIndex * theta + noiseValue;
            }
        }
        throw new IllegalStateException("# of resample exceeds MAX_RESAMPLE = " + MAX_RESAMPLE);
    }

    @Override
    public double getPolicyEpsilon(double x1, double x2) {
        double lowerBound = localMapRealLdpConfig.getLowerBound();
        double upperBound = localMapRealLdpConfig.getUpperBound();
        assert x1 >= lowerBound && x1 <= upperBound : "x1 must be in range [" + lowerBound + ", " + upperBound + "]";
        assert x2 >= lowerBound && x2 <= upperBound : "x2 must be in range [" + lowerBound + ", " + upperBound + "]";
        int partitionIndex1 = getPartitionIndex(x1);
        int partitionIndex2 = getPartitionIndex(x2);
        if (partitionIndex1 != partitionIndex2) {
            // For any pair of values x, x' are in different partitions, x, x' can be distinguished.
            return Double.MAX_VALUE;
        } else {
            // For any pair of values x, x' are in the same partition, x, x' satisfies ε_ner -dLDP,
            // where |x' − x| ≤ t ≤ θ, ε_ner > 0.
            return Math.abs(x1 - x2) * localMapRealLdpConfig.getBaseEpsilon();
        }
    }

    private double getPartitionValue(double value) {
        // 计算分区内的值：如果真实值大于0，正常处理；如果真实值小于0，则取模后也小于0，需要再往右移动一个θ
        return (value % theta) >= 0 ? (value % theta) : (value % theta + theta);
    }

    private int getPartitionIndex(double value) {
        // 计算分区索引值：如果真实值大于0，正常处理；如果真实值小于0，则除法后也小于0，需要再往左移动一个索引值
        return (value >= 0 || Precision.equals(value % theta, 0, DoubleUtils.PRECISION)) ?
            (int) (value / theta) : (int) (value / theta - 1);
    }
}

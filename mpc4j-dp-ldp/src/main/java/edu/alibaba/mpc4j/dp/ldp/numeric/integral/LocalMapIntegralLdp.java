package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

import edu.alibaba.mpc4j.common.sampler.integral.geometric.GeometricSampler;
import edu.alibaba.mpc4j.common.sampler.integral.geometric.JdkGeometricSampler;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;

/**
 * 本地映射整数LDP机制。
 *
 * @author Weiran Liu
 * @date 2022/5/4
 */
class LocalMapIntegralLdp implements IntegralLdp {
    /**
     * 默认重采样次数
     */
    private static final int MAX_RESAMPLE = 1 << 20;
    /**
     * 配置项
     */
    private LocalMapIntegralLdpConfig integralLdpConfig;
    /**
     * 下界
     */
    private int lowerBound;
    /**
     * 上界
     */
    private int upperBound;
    /**
     * 分区长度θ
     */
    private int theta;
    /**
     * 几何分布采样器
     */
    private GeometricSampler geometricSampler;

    @Override
    public void setup(LdpConfig ldpConfig) {
        assert ldpConfig instanceof LocalMapIntegralLdpConfig;
        integralLdpConfig = (LocalMapIntegralLdpConfig) ldpConfig;
        // 设置上下界
        lowerBound = integralLdpConfig.getLowerBound();
        upperBound = integralLdpConfig.getUpperBound();
        // 设置分区长度θ
        theta = integralLdpConfig.getTheta();
        // 计算放缩系数b = 2 / ε
        double b = 2.0 / integralLdpConfig.getBaseEpsilon();
        // 设置几何分布采样器
        geometricSampler = new JdkGeometricSampler(integralLdpConfig.getRandom(), 0, b);
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        integralLdpConfig.getRandom().setSeed(seed);
    }

    @Override
    public String getMechanismName() {
        return "[" + getLowerBound() + ", " + getUpperBound() + "], " +
            "(θ = " + theta + ", ε = " + integralLdpConfig.getBaseEpsilon() + ")-" +
            getClass().getSimpleName();
    }

    @Override
    public LdpConfig getLdpConfig() {
        return integralLdpConfig;
    }

    @Override
    public int randomize(int value) {
        assert value >= lowerBound && value <= upperBound : "value must be in range [" + lowerBound + ", " + upperBound + "]";
        // 计算分区内的值：如果真实值大于0，正常处理；如果真实值小于0，则取模后也小于0，需要再往右移动一个θ
        int partitionValue = getPartitionValue(value);
        int partitionIndex = getPartitionIndex(value);
        // 在partitionValue上应用敏感度等于1的拉普拉斯机制，并验证结果是否在[0, θ)间，如果不满足，则重采样
        int count = 0;
        while (count <= MAX_RESAMPLE) {
            count++;
            int noiseValue = geometricSampler.sample() + partitionValue;
            if (noiseValue >= 0 && noiseValue < theta) {
                return partitionIndex * theta + noiseValue;
            }
        }
        throw new IllegalStateException("# of resample exceeds MAX_RESAMPLE = " + MAX_RESAMPLE);
    }

    @Override
    public double getPolicyEpsilon(int x1, int x2) {
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
            return Math.abs(x1 - x2) * integralLdpConfig.getBaseEpsilon();
        }
    }

    private int getPartitionValue(int value) {
        // 计算分区内的值：如果真实值大于0，正常处理；如果真实值小于0，则取模后也小于0，需要再往右移动一个θ
        return (value % theta) >= 0 ? (value % theta) : (value % theta + theta);
    }

    private int getPartitionIndex(int value) {
        // 计算分区索引值：如果真实值大于0，正常处理；如果真实值小于0，则除法后也小于0，需要再往左移动一个索引值
        return (value >= 0 || value % theta == 0) ? (value / theta) : (value / theta - 1);
    }
}

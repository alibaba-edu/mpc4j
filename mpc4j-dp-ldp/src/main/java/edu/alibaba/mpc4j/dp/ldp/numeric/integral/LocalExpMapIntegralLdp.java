package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.BoundIntegralCdp;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.BoundIntegralCdpFactory;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.ExpBoundIntegralCdpConfig;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;

/**
 * 本地映射指数整数LDP机制。
 *
 * @author Weiran Liu
 * @date 2022/7/5
 */
class LocalExpMapIntegralLdp implements IntegralLdp {
    /**
     * 配置项
     */
    private LocalExpMapIntegralLdpConfig integralLdpConfig;
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
     * 有界整数机制
     */
    private BoundIntegralCdp boundIntegralCdp;

    @Override
    public void setup(LdpConfig ldpConfig) {
        assert ldpConfig instanceof LocalExpMapIntegralLdpConfig;
        integralLdpConfig = (LocalExpMapIntegralLdpConfig) ldpConfig;
        // 设置上下界
        lowerBound = integralLdpConfig.getLowerBound();
        upperBound = integralLdpConfig.getUpperBound();
        // 设置分区长度θ
        theta = integralLdpConfig.getTheta();
        // 设置有界指数CDP机制
        ExpBoundIntegralCdpConfig expBoundIntegralCdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(integralLdpConfig.getBaseEpsilon() / 2, 1, 0, theta - 1)
            .setRandom(integralLdpConfig.getRandom())
            .build();
        boundIntegralCdp = BoundIntegralCdpFactory.createInstance(expBoundIntegralCdpConfig);
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
        // 分区内加噪声后平移
        return partitionIndex * theta + boundIntegralCdp.randomize(partitionValue);
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

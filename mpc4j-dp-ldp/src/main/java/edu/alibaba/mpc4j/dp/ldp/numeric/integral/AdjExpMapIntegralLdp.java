package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.BoundIntegralCdp;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.BoundIntegralCdpFactory;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.ExpBoundIntegralCdpConfig;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import org.apache.commons.math3.util.Precision;

/**
 * 调整映射指数整数LDP机制。
 *
 * @author Weiran Liu
 * @date 2022/7/5
 */
class AdjExpMapIntegralLdp implements IntegralLdp {
    /**
     * 配置项
     */
    private AdjExpMapIntegralLdpConfig integralLdpConfig;
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
     * 划分比例α
     */
    private double alpha;
    /**
     * ε_ner
     */
    private double valueEpsilon;
    /**
     * ε_prt
     */
    private double partitionEpsilon;
    /**
     * 取值采样器
     */
    private BoundIntegralCdp valueBoundIntegralCdp;
    /**
     * 分区采样器
     */
    private BoundIntegralCdp partitionBoundIntegralCdp;

    @Override
    public void setup(LdpConfig ldpConfig) {
        assert ldpConfig instanceof AdjExpMapIntegralLdpConfig;
        integralLdpConfig = (AdjExpMapIntegralLdpConfig) ldpConfig;
        // 设置上下界
        lowerBound = integralLdpConfig.getLowerBound();
        upperBound = integralLdpConfig.getUpperBound();
        // 设置分区长度θ和划分比例α
        theta = integralLdpConfig.getTheta();
        alpha = integralLdpConfig.getAlpha();
        // 设置取值采样器上界
        int partitionLowerBound = getPartitionIndex(lowerBound);
        int partitionUpperBound = getPartitionIndex(upperBound);
        // 计算ε_ner和ε_prt
        int d = upperBound - lowerBound;
        double baseEpsilon = integralLdpConfig.getBaseEpsilon();
        // ε_ner = ε / (α + θ / |D|)
        valueEpsilon = baseEpsilon / (alpha + 1.0 / Math.floor((double)d / theta));
        // 初始化取值采样器
        ExpBoundIntegralCdpConfig valueBoundIntegralCdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(valueEpsilon / 2, 1, 0, theta - 1)
            .setRandom(integralLdpConfig.getRandom())
            .build();
        valueBoundIntegralCdp = BoundIntegralCdpFactory.createInstance(valueBoundIntegralCdpConfig);
        // ε_prt = α * θ * ε_ner
        partitionEpsilon = alpha * theta * valueEpsilon;
        // 初始化分区采样器
        ExpBoundIntegralCdpConfig partitionBoundIntegralCdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(partitionEpsilon / 2, 1, partitionLowerBound, partitionUpperBound)
            .setRandom(integralLdpConfig.getRandom())
            .build();
        partitionBoundIntegralCdp = BoundIntegralCdpFactory.createInstance(partitionBoundIntegralCdpConfig);
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        integralLdpConfig.getRandom().setSeed(seed);
    }

    @Override
    public String getMechanismName() {
        return "[" + getLowerBound() + ", " + getUpperBound() + "], " +
            "(θ = " + theta + ", α = " + alpha + ", ε = " + integralLdpConfig.getBaseEpsilon() + ")-" +
            getClass().getSimpleName();
    }

    @Override
    public LdpConfig getLdpConfig() {
        return integralLdpConfig;
    }

    @Override
    public int randomize(int value) {
        assert value >= lowerBound && value <= upperBound : "value must be in range [" + lowerBound + ", " + upperBound + "]";
        int partitionIndex = getPartitionIndex(value);
        // 在partitionValue上应用敏感度等于1的几何机制
        int noisePartitionIndex = partitionBoundIntegralCdp.randomize(partitionIndex);
        if (noisePartitionIndex > partitionIndex) {
            // 如果随机索引值在真实索引值右侧，则分区内左边界采样
            return valueBoundIntegralCdp.randomize(0) + noisePartitionIndex * theta;
        } else if (noisePartitionIndex < partitionIndex) {
            // 如果随机索引值在真实索引值左侧，则分区内右边界采样
            return valueBoundIntegralCdp.randomize(theta - 1) + noisePartitionIndex * theta;
        } else {
            // 计算分区内的值：如果真实值大于0，正常处理；如果真实值小于0，则取模后也小于0，需要再往右移动一个θ
            int partitionValue = getPartitionValue(value);
            return valueBoundIntegralCdp.randomize(partitionValue) + noisePartitionIndex * theta;
        }
    }

    @Override
    public double getPolicyEpsilon(int x1, int x2) {
        assert x1 >= lowerBound && x1 <= upperBound : "x1 must be in range [" + lowerBound + ", " + upperBound + "]";
        assert x2 >= lowerBound && x2 <= upperBound : "x2 must be in range [" + lowerBound + ", " + upperBound + "]";
        return Math.floor((double)Math.abs(x1 - x2) / theta) * partitionEpsilon + theta * valueEpsilon;
    }

    private int getPartitionValue(int value) {
        if ((value % theta) >= 0) {
            // 如果真实值大于0，正常处理
            return value % theta;
        }
        // 如果真实值小于0，则取模后小于等于0，小于0时需要再往右移动一个θ
        int partitionValue = value % theta;
        return partitionValue == 0 ? partitionValue : partitionValue + theta;
    }

    private int getPartitionIndex(double value) {
        // 计算分区索引值：如果真实值大于0，正常处理；如果真实值小于0，则除法后也小于0，需要再往左移动一个索引值
        return (value >= 0 || Precision.equals(value % theta, 0, DoubleUtils.PRECISION)) ?
            (int) (value / theta) : (int) (value / theta - 1);
    }
}

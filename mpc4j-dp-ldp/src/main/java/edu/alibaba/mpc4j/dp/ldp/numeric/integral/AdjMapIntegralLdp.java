package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

import edu.alibaba.mpc4j.common.sampler.integral.geometric.GeometricSampler;
import edu.alibaba.mpc4j.common.sampler.integral.geometric.JdkGeometricSampler;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import org.apache.commons.math3.util.Precision;

/**
 * 调整映射整数LDP机制。
 *
 * @author Weiran Liu
 * @date 2022/5/4
 */
class AdjMapIntegralLdp implements IntegralLdp {
    /**
     * 默认重采样次数
     */
    private static final int MAX_RESAMPLE = 1 << 20;
    /**
     * 配置项
     */
    private AdjMapIntegralLdpConfig integralLdpConfig;
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
     * 取值采样器下界
     */
    private int partitionLowerBound;
    /**
     * 取值采样器下界
     */
    private int partitionUpperBound;
    /**
     * ε_ner
     */
    private double neighborEpsilon;
    /**
     * ε_prt
     */
    private double partitionEpsilon;
    /**
     * 取值几何分布采样器
     */
    private GeometricSampler valueGeometricSampler;
    /**
     * 分区几何分布采样器
     */
    private GeometricSampler partitionGeometricSampler;

    @Override
    public void setup(LdpConfig ldpConfig) {
        assert ldpConfig instanceof AdjMapIntegralLdpConfig;
        integralLdpConfig = (AdjMapIntegralLdpConfig) ldpConfig;
        // 设置上下界
        lowerBound = integralLdpConfig.getLowerBound();
        upperBound = integralLdpConfig.getUpperBound();
        // 设置分区长度θ和划分比例α
        theta = integralLdpConfig.getTheta();
        alpha = integralLdpConfig.getAlpha();
        // 设置取值采样器上界
        partitionLowerBound = getPartitionIndex(lowerBound);
        partitionUpperBound = getPartitionIndex(upperBound);
        // 计算ε_ner和ε_prt
        int d = upperBound - lowerBound;
        double baseEpsilon = integralLdpConfig.getBaseEpsilon();
        // ε_ner = ε / (α + θ / |D|)
        neighborEpsilon = baseEpsilon / (alpha + 1.0 / Math.floor((double)d / theta));
        // b_ner = 2 / ε_ner
        double neighborB = 2.0 / neighborEpsilon;
        valueGeometricSampler = new JdkGeometricSampler(integralLdpConfig.getRandom(), 0, neighborB);
        // ε_prt = α * θ * ε_ner
        partitionEpsilon = alpha * theta * neighborEpsilon;
        double partitionB = 2.0 / partitionEpsilon;
        partitionGeometricSampler = new JdkGeometricSampler(integralLdpConfig.getRandom(), 0, partitionB);
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
        int noisePartitionIndex = randomizePartitionIndex(partitionIndex);
        if (noisePartitionIndex > partitionIndex) {
            // 如果随机索引值在真实索引值右侧，则分区内左边界采样
            return randomizeValue(0) + noisePartitionIndex * theta;
        } else if (noisePartitionIndex < partitionIndex) {
            // 如果随机索引值在真实索引值左侧，则分区内右边界采样
            return randomizeValue(theta - 1) + noisePartitionIndex * theta;
        } else {
            // 计算分区内的值：如果真实值大于0，正常处理；如果真实值小于0，则取模后也小于0，需要再往右移动一个θ
            int partitionValue = getPartitionValue(value);
            return randomizeValue(partitionValue) + noisePartitionIndex * theta;
        }
    }

    @Override
    public double getPolicyEpsilon(int x1, int x2) {
        assert x1 >= lowerBound && x1 <= upperBound : "x1 must be in range [" + lowerBound + ", " + upperBound + "]";
        assert x2 >= lowerBound && x2 <= upperBound : "x2 must be in range [" + lowerBound + ", " + upperBound + "]";
        return Math.floor((double)Math.abs(x1 - x2) / theta) * partitionEpsilon + theta * neighborEpsilon;
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

    private int randomizePartitionIndex(int partitionIndex) {
        int count = 0;
        while (count <= MAX_RESAMPLE) {
            count++;
            int noisePartition = partitionGeometricSampler.sample() + partitionIndex;
            if (noisePartition >= partitionLowerBound && noisePartition <= partitionUpperBound) {
                return noisePartition;
            }
        }
        throw new IllegalStateException("# of resample exceeds MAX_RESAMPLE = " + MAX_RESAMPLE);
    }

    private int randomizeValue(int value) {
        int count = 0;
        while (count <= MAX_RESAMPLE) {
            count++;
            int noiseValue = valueGeometricSampler.sample() + value;
            if (noiseValue >= 0 && noiseValue < theta) {
                return noiseValue;
            }
        }
        throw new IllegalStateException("# of resample exceeds MAX_RESAMPLE = " + MAX_RESAMPLE);
    }
}

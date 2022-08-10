package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.dp.ldp.range.RangeLdp;
import edu.alibaba.mpc4j.dp.ldp.range.RangeLdpFactory;

/**
 * 朴素范围整数LDP机制。基本思想：将原始输入值映射到[-1, 1]之间，随机化处理后再映射回去。
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
class NaiveRangeIntegralLdp implements IntegralLdp {
    /**
     * 配置项
     */
    private NaiveRangeIntegralLdpConfig naiveRangeIntegralLdpConfig;
    /**
     * 范围CDP机制
     */
    private RangeLdp rangeLdp;
    /**
     * 范围
     */
    private double range;
    /**
     * 缩小比率
     */
    private double shrinkRate;
    /**
     * 放大比率
     */
    private double magnifyRate;

    @Override
    public void setup(LdpConfig ldpConfig) {
        assert ldpConfig instanceof NaiveRangeIntegralLdpConfig;
        naiveRangeIntegralLdpConfig = (NaiveRangeIntegralLdpConfig)ldpConfig;
        // 初始化离散整数型差分隐私机制
        rangeLdp = RangeLdpFactory.createInstance(naiveRangeIntegralLdpConfig.getRangeLdpConfig());
        range = naiveRangeIntegralLdpConfig.getUpperBound() - naiveRangeIntegralLdpConfig.getLowerBound();
        shrinkRate = 2.0 / range;
        magnifyRate = range / 2.0;
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        rangeLdp.reseed(seed);
    }

    @Override
    public LdpConfig getLdpConfig() {
        return naiveRangeIntegralLdpConfig;
    }

    @Override
    public int randomize(int value) {
        int lowerBound = naiveRangeIntegralLdpConfig.getLowerBound();
        int upperBound = naiveRangeIntegralLdpConfig.getUpperBound();
        assert value >= lowerBound && value <= upperBound : "value must be in range [" + lowerBound + ", " + upperBound + "]";
        // 将真实值压缩到[-range / 2, range / 2]之间，随后放缩到[-1, 1]之间
        double normalizedValue = (value - lowerBound - range / 2) * shrinkRate;
        // 采样
        double normalizedRandomValue = rangeLdp.randomize(normalizedValue);
        // 将随机值放大到原来的比率
        return (int)Math.round(normalizedRandomValue * magnifyRate + lowerBound + range / 2);
    }

    @Override
    public double getPolicyEpsilon(int x1, int x2) {
        return rangeLdp.getEpsilon();
    }


    @Override
    public String getMechanismName() {
        return "[" + getLowerBound() + ", " + getUpperBound() + "])-"
            + getClass().getSimpleName() + "(" + rangeLdp.getMechanismName() + ")";
    }
}

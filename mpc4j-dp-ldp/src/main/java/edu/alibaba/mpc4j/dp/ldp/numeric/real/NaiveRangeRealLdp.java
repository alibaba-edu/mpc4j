package edu.alibaba.mpc4j.dp.ldp.numeric.real;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;
import edu.alibaba.mpc4j.dp.ldp.range.RangeLdp;
import edu.alibaba.mpc4j.dp.ldp.range.RangeLdpFactory;
import org.apache.commons.math3.util.Precision;

/**
 * 朴素范围实数LDP机制。基本思想：将原始输入值映射到[-1, 1]之间，随机化处理后再映射回去。
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
class NaiveRangeRealLdp implements RealLdp {
    /**
     * 配置项
     */
    private NaiveRangeRealLdpConfig naiveRangeRealLdpConfig;
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
        assert ldpConfig instanceof NaiveRangeRealLdpConfig;
        naiveRangeRealLdpConfig = (NaiveRangeRealLdpConfig)ldpConfig;
        // 初始化离散整数型差分隐私机制
        rangeLdp = RangeLdpFactory.createInstance(naiveRangeRealLdpConfig.getRangeLdpConfig());
        range = naiveRangeRealLdpConfig.getUpperBound() - naiveRangeRealLdpConfig.getLowerBound();
        shrinkRate = 2.0 / range;
        magnifyRate = range / 2.0;
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        rangeLdp.reseed(seed);
    }

    @Override
    public LdpConfig getLdpConfig() {
        return naiveRangeRealLdpConfig;
    }

    @Override
    public double randomize(double value) {
        double lowerBound = naiveRangeRealLdpConfig.getLowerBound();
        double upperBound = naiveRangeRealLdpConfig.getUpperBound();
        assert value >= lowerBound && value <= upperBound : "value must be in range [" + lowerBound + ", " + upperBound + "]";
        // 如果上下界相等，则返回确定值
        if (Precision.equals(lowerBound, upperBound, DoubleUtils.PRECISION)) {
            return lowerBound;
        }

        // 将真实值压缩到[-range / 2, range / 2]之间，随后放缩到[-1, 1]之间
        double normalizedValue = (value - lowerBound - range / 2) * shrinkRate;
        // 采样
        double normalizedRandomValue = rangeLdp.randomize(normalizedValue);
        // 将随机值放大回真实的范围
        return normalizedRandomValue * magnifyRate + lowerBound + range / 2;
    }

    @Override
    public double getPolicyEpsilon(double x1, double x2) {
        return rangeLdp.getEpsilon();
    }


    @Override
    public String getMechanismName() {
        return "[" + getLowerBound() + ", " + getUpperBound() + "]-"
            + getClass().getSimpleName() + "(" + rangeLdp.getMechanismName() + ")";
    }
}

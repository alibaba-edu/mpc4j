package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.BoundIntegralCdp;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.BoundIntegralCdpFactory;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.ExpBoundIntegralCdpConfig;
import edu.alibaba.mpc4j.dp.ldp.LdpConfig;

/**
 * 全局映射指数整数LDP机制。
 *
 * @author Weiran Liu
 * @date 2022/7/4
 */
class GlobalExpMapIntegralLdp implements IntegralLdp {
    /**
     * 配置项
     */
    private GlobalExpMapIntegralLdpConfig integralLdpConfig;
    /**
     * 下界
     */
    private int lowerBound;
    /**
     * 上界
     */
    private int upperBound;
    /**
     * 有界整数机制
     */
    private BoundIntegralCdp boundIntegralCdp;

    @Override
    public void setup(LdpConfig ldpConfig) {
        assert ldpConfig instanceof GlobalExpMapIntegralLdpConfig;
        integralLdpConfig = (GlobalExpMapIntegralLdpConfig) ldpConfig;
        // 设置上下界
        lowerBound = integralLdpConfig.getLowerBound();
        upperBound = integralLdpConfig.getUpperBound();
        // 构建有界整数CDP机制
        ExpBoundIntegralCdpConfig expBoundIntegralCdpConfig = new ExpBoundIntegralCdpConfig
            .Builder(integralLdpConfig.getBaseEpsilon() / 2, 1, lowerBound, upperBound)
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
            "(ε = " + integralLdpConfig.getBaseEpsilon() +  ")-" +
            getClass().getSimpleName() ;
    }

    @Override
    public LdpConfig getLdpConfig() {
        return integralLdpConfig;
    }

    @Override
    public int randomize(int value) {
        return boundIntegralCdp.randomize(value);
    }

    @Override
    public double getPolicyEpsilon(int x1, int x2) {
        assert x1 >= lowerBound && x1 <= upperBound : "x1 must be in range [" + lowerBound + ", " + upperBound + "]";
        assert x2 >= lowerBound && x2 <= upperBound : "x2 must be in range [" + lowerBound + ", " + upperBound + "]";
        // Global-map provides ε-dLDP privacy guarantee for any pair of values x,x' ∈ D, where |x' − x| = t, and t, ε > 0.
        return Math.abs(x1 - x2) * integralLdpConfig.getBaseEpsilon();
    }
}

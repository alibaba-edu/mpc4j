package edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound;

/**
 * 有界整数CDP机制工厂类。
 *
 * @author Weiran Liu
 * @date 2022/4/24
 */
public class BoundIntegralCdpFactory {
    /**
     * 私有构造函数
     */
    private BoundIntegralCdpFactory() {
        // empty
    }

    /**
     * 构造有界整数CDP机制。
     *
     * @param boundIntegralCdpConfig 配置项。
     * @return 有界整数CDP机制。
     */
    public static BoundIntegralCdp createInstance(BoundIntegralCdpConfig boundIntegralCdpConfig) {
        if (boundIntegralCdpConfig instanceof ExpBoundIntegralCdpConfig) {
            ExpBoundIntegralCdp expBoundIntegralCdp = new ExpBoundIntegralCdp();
            expBoundIntegralCdp.setup(boundIntegralCdpConfig);
            return expBoundIntegralCdp;
        }
        if (boundIntegralCdpConfig instanceof NaiveBoundIntegralCdpConfig) {
            NaiveBoundIntegralCdp naiveBoundIntegralCdp = new NaiveBoundIntegralCdp();
            naiveBoundIntegralCdp.setup(boundIntegralCdpConfig);
            return naiveBoundIntegralCdp;
        }
        if (boundIntegralCdpConfig instanceof Base2ExpBoundIntegralCdpConfig) {
            Base2ExpBoundIntegralCdp base2ExpBoundIntegralCdp = new Base2ExpBoundIntegralCdp();
            base2ExpBoundIntegralCdp.setup(boundIntegralCdpConfig);
            return base2ExpBoundIntegralCdp;
        }
        throw new IllegalArgumentException("Invalid BoundIntegralCdpConfig: " + boundIntegralCdpConfig.getClass().getSimpleName());
    }
}

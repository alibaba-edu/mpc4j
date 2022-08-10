package edu.alibaba.mpc4j.dp.cdp.numeric.integral;

import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.BoundIntegralCdpConfig;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.BoundIntegralCdpFactory;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.UnboundIntegralCdpConfig;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.UnboundIntegralCdpFactory;

/**
 * 整数CDP机制工厂类。
 *
 * @author Weiran Liu
 * @date 2022/4/23
 */
public class IntegralCdpFactory {
    /**
     * 私有构造函数
     */
    private IntegralCdpFactory() {
        // empty
    }

    /**
     * 构造整数CDP机制。
     *
     * @param integralCdpConfig 配置项。
     * @return 整数CDP机制。
     */
    public static IntegralCdp createInstance(IntegralCdpConfig integralCdpConfig) {
        if (integralCdpConfig instanceof UnboundIntegralCdpConfig) {
            return UnboundIntegralCdpFactory.createInstance((UnboundIntegralCdpConfig)integralCdpConfig);
        }
        if (integralCdpConfig instanceof BoundIntegralCdpConfig) {
            return BoundIntegralCdpFactory.createInstance((BoundIntegralCdpConfig) integralCdpConfig);
        }
        throw new IllegalArgumentException("Invalid IntegralCdpConfig: " + integralCdpConfig.getClass().getSimpleName());
    }
}

package edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound;

import edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.geometric.GeometricCdpConfig;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.geometric.GeometricCdpFactory;

/**
 * 无界整数CDP机制工厂类。
 *
 * @author Weiran Liu
 * @date 2022/4/24
 */
public class UnboundIntegralCdpFactory {
    /**
     * 私有构造函数
     */
    private UnboundIntegralCdpFactory() {
        // empty
    }

    /**
     * 构造无界整数CDP机制。
     *
     * @param unboundIntegralCdpConfig 配置项。
     * @return 无界整数CDP机制。
     */
    public static UnboundIntegralCdp createInstance(UnboundIntegralCdpConfig unboundIntegralCdpConfig) {
        if (unboundIntegralCdpConfig instanceof GeometricCdpConfig) {
            return GeometricCdpFactory.createInstance((GeometricCdpConfig) unboundIntegralCdpConfig);
        }
        throw new IllegalArgumentException("Invalid UnboundIntegralCdpConfig: " + unboundIntegralCdpConfig.getClass().getSimpleName());
    }
}

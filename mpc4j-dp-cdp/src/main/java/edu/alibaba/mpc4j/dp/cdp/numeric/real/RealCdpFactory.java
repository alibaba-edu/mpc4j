package edu.alibaba.mpc4j.dp.cdp.numeric.real;

import edu.alibaba.mpc4j.dp.cdp.numeric.real.bound.BoundRealCdpConfig;
import edu.alibaba.mpc4j.dp.cdp.numeric.real.bound.BoundRealCdpFactory;
import edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound.UnboundRealCdpConfig;
import edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound.UnboundRealCdpFactory;

/**
 * 实数CDP机制工厂。
 *
 * @author Weiran Liu
 * @date 2022/4/20
 */
public class RealCdpFactory {
    /**
     * 私有构造函数
     */
    private RealCdpFactory() {
        // empty
    }

    /**
     * 构造实数CDP机制。
     *
     * @param realCdpConfig 配置项。
     * @return 实数CDP机制。
     */
    public static RealCdp createInstance(RealCdpConfig realCdpConfig) {
        if (realCdpConfig instanceof UnboundRealCdpConfig) {
            return UnboundRealCdpFactory.createInstance((UnboundRealCdpConfig) realCdpConfig);
        }
        if (realCdpConfig instanceof BoundRealCdpConfig) {
            return BoundRealCdpFactory.createInstance((BoundRealCdpConfig) realCdpConfig);
        }
        throw new IllegalArgumentException("Illegal RealCdpConfig: " + realCdpConfig.getClass().getSimpleName());
    }
}

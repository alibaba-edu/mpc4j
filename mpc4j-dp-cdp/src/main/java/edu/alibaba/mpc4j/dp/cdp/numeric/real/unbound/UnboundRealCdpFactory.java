package edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound;

/**
 * 无界实数CDP机制工厂类。
 *
 * @author Weiran Liu
 * @date 2022/4/24
 */
public class UnboundRealCdpFactory {
    /**
     * 私有构造函数
     */
    private UnboundRealCdpFactory() {
        // empty
    }

    /**
     * 构造无界实数CDP机制。
     *
     * @param unboundRealCdpConfig 配置项。
     * @return 无界实数CDP机制。
     */
    public static UnboundRealCdp createInstance(UnboundRealCdpConfig unboundRealCdpConfig) {
        if (unboundRealCdpConfig instanceof ApacheLaplaceCdpConfig) {
            ApacheLaplaceCdp apacheLaplaceCdp = new ApacheLaplaceCdp();
            apacheLaplaceCdp.setup(unboundRealCdpConfig);
            return apacheLaplaceCdp;
        }
        if (unboundRealCdpConfig instanceof GoogleLaplaceCdpConfig) {
            GoogleLaplaceCdp googleLaplaceCdp = new GoogleLaplaceCdp();
            googleLaplaceCdp.setup(unboundRealCdpConfig);
            return googleLaplaceCdp;
        }
        if (unboundRealCdpConfig instanceof StaircaseCdpConfig) {
            StaircaseCdp staircaseCdp = new StaircaseCdp();
            staircaseCdp.setup(unboundRealCdpConfig);
            return staircaseCdp;
        }
        throw new IllegalArgumentException("Invalid UnboundRealCdpConfig: " + unboundRealCdpConfig.getClass().getSimpleName());
    }

}

package edu.alibaba.mpc4j.dp.cdp.numeric.real.bound;

/**
 * 有界实数CDP机制工厂类。
 *
 * @author Weiran Liu
 * @date 2022/4/25
 */
public class BoundRealCdpFactory {
    /**
     * 私有构造函数
     */
    private BoundRealCdpFactory() {
        // empty
    }

    /**
     * 构造有界实数CDP机制。
     *
     * @param boundRealCdpConfig 配置项。
     * @return 有界实数CDP机制。
     */
    public static BoundRealCdp createInstance(BoundRealCdpConfig boundRealCdpConfig) {
        if (boundRealCdpConfig instanceof NaiveBoundRealCdpConfig) {
            NaiveBoundRealCdp naiveBoundRealCdp = new NaiveBoundRealCdp();
            naiveBoundRealCdp.setup(boundRealCdpConfig);
            return naiveBoundRealCdp;
        }
        throw new IllegalArgumentException("Invalid BoundRealCdpConfig: " + boundRealCdpConfig.getClass().getSimpleName());
    }
}

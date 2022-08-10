package edu.alibaba.mpc4j.dp.ldp.numeric.real;

/**
 * 实数LDP机制工厂类。
 *
 * @author Weiran Liu
 * @date 2022/4/28
 */
public class RealLdpFactory {
    /**
     * 私有构造函数
     */
    private RealLdpFactory() {
        // empty
    }

    /**
     * 实数LDP机制类型
     */
    public enum RealLdpType {
        /**
         * 朴素范围机制
         */
        NAIVE_RANGE,
        /**
         * 全局映射机制
         */
        GLOBAL_MAP,
        /**
         * 本地映射机制
         */
        LOCAL_MAP,
        /**
         * 调整映射机制
         */
        ADJ_MAP,
    }

    /**
     * 构造实数LDP机制。
     *
     * @param realLdpConfig 配置项。
     * @return 实数LDP机制。
     */
    public static RealLdp createInstance(RealLdpConfig realLdpConfig) {
        if (realLdpConfig instanceof NaiveRangeRealLdpConfig) {
            NaiveRangeRealLdp naiveRangeRealLdp = new NaiveRangeRealLdp();
            naiveRangeRealLdp.setup(realLdpConfig);
            return naiveRangeRealLdp;
        }
        if (realLdpConfig instanceof GlobalMapRealLdpConfig) {
            GlobalMapRealLdp globalMapRealLdp = new GlobalMapRealLdp();
            globalMapRealLdp.setup(realLdpConfig);
            return globalMapRealLdp;
        }
        if (realLdpConfig instanceof LocalMapRealLdpConfig) {
            LocalMapRealLdp localMapRealLdp = new LocalMapRealLdp();
            localMapRealLdp.setup(realLdpConfig);
            return localMapRealLdp;
        }
        if (realLdpConfig instanceof AdjMapRealLdpConfig) {
            AdjMapRealLdp adjMapRealLdp = new AdjMapRealLdp();
            adjMapRealLdp.setup(realLdpConfig);
            return adjMapRealLdp;
        }
        throw new IllegalArgumentException("Illegal RealLdpConfig: " + realLdpConfig.getClass().getSimpleName());
    }
}

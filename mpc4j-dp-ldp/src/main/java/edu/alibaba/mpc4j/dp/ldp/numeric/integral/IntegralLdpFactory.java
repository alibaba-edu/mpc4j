package edu.alibaba.mpc4j.dp.ldp.numeric.integral;

/**
 * 整数LDP机制工厂类。
 *
 * @author Weiran Liu
 * @date 2022/4/27
 */
public class IntegralLdpFactory {
    /**
     * 私有构造函数
     */
    private IntegralLdpFactory() {
        // empty
    }

    /**
     * 整数LDP机制类型
     */
    public enum IntegralLdpType {
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
     * 构造整数LDP机制。
     *
     * @param integralLdpConfig 配置项。
     * @return 整数LDP机制。
     */
    public static IntegralLdp createInstance(IntegralLdpConfig integralLdpConfig) {
        if (integralLdpConfig instanceof NaiveRangeIntegralLdpConfig) {
            NaiveRangeIntegralLdp naiveRangeIntegralLdp = new NaiveRangeIntegralLdp();
            naiveRangeIntegralLdp.setup(integralLdpConfig);
            return naiveRangeIntegralLdp;
        }
        if (integralLdpConfig instanceof GlobalMapIntegralLdpConfig) {
            GlobalMapIntegralLdp globalMapIntegralLdp = new GlobalMapIntegralLdp();
            globalMapIntegralLdp.setup(integralLdpConfig);
            return globalMapIntegralLdp;
        }
        if (integralLdpConfig instanceof GlobalExpMapIntegralLdpConfig) {
            GlobalExpMapIntegralLdp globalExpMapIntegralLdp = new GlobalExpMapIntegralLdp();
            globalExpMapIntegralLdp.setup(integralLdpConfig);
            return globalExpMapIntegralLdp;
        }
        if (integralLdpConfig instanceof LocalMapIntegralLdpConfig) {
            LocalMapIntegralLdp localMapIntegralLdp = new LocalMapIntegralLdp();
            localMapIntegralLdp.setup(integralLdpConfig);
            return localMapIntegralLdp;
        }
        if (integralLdpConfig instanceof LocalExpMapIntegralLdpConfig) {
            LocalExpMapIntegralLdp localExpMapIntegralLdp = new LocalExpMapIntegralLdp();
            localExpMapIntegralLdp.setup(integralLdpConfig);
            return localExpMapIntegralLdp;
        }
        if (integralLdpConfig instanceof AdjMapIntegralLdpConfig) {
            AdjMapIntegralLdp adjMapIntegralLdp = new AdjMapIntegralLdp();
            adjMapIntegralLdp.setup(integralLdpConfig);
            return adjMapIntegralLdp;
        }
        if (integralLdpConfig instanceof AdjExpMapIntegralLdpConfig) {
            AdjExpMapIntegralLdp adjExpMapIntegralLdp = new AdjExpMapIntegralLdp();
            adjExpMapIntegralLdp.setup(integralLdpConfig);
            return adjExpMapIntegralLdp;
        }
        throw new IllegalArgumentException("Illegal IntegralLdpConfig: " + integralLdpConfig.getClass().getSimpleName());
    }
}

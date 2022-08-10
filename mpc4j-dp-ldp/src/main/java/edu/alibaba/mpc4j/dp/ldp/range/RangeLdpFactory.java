package edu.alibaba.mpc4j.dp.ldp.range;

/**
 * 范围LDP机制工厂类。
 *
 * @author Weiran Liu
 * @date 2022/4/26
 */
public class RangeLdpFactory {
    /**
     * 私有构造函数
     */
    private RangeLdpFactory() {
        // empty
    }

    /**
     * 范围LDP机制类型
     */
    public enum RangeLdpType {
        /**
         * Apache拉普拉斯机制
         */
        APACHE_LAPLACE,
        /**
         * minimax机制
         */
        MINIMAX,
        /**
         * 分段机制
         */
        PIECEWISE,
        /**
         * 混合机制
         */
        HYBRID,
    }

    /**
     * 构造范围LDP机制。
     *
     * @param rangeLdpConfig 配置项。
     * @return 范围LDP机制。
     */
    public static RangeLdp createInstance(RangeLdpConfig rangeLdpConfig) {
        if (rangeLdpConfig instanceof ApacheLaplaceLdpConfig) {
            ApacheLaplaceLdp apacheLaplaceLdp = new ApacheLaplaceLdp();
            apacheLaplaceLdp.setup(rangeLdpConfig);
            return apacheLaplaceLdp;
        }
        if (rangeLdpConfig instanceof GoogleLaplaceLdpConfig) {
            GoogleLaplaceLdp googleLaplaceLdp = new GoogleLaplaceLdp();
            googleLaplaceLdp.setup(rangeLdpConfig);
            return googleLaplaceLdp;
        }
        if (rangeLdpConfig instanceof MinimaxLdpConfig) {
            MinimaxLdp minimaxLdp = new MinimaxLdp();
            minimaxLdp.setup(rangeLdpConfig);
            return minimaxLdp;
        }
        if (rangeLdpConfig instanceof PiecewiseLdpConfig) {
            PiecewiseLdp piecewiseLdp = new PiecewiseLdp();
            piecewiseLdp.setup(rangeLdpConfig);
            return piecewiseLdp;
        }
        if (rangeLdpConfig instanceof HybridLdpConfig) {
            HybridLdp hybridLdp = new HybridLdp();
            hybridLdp.setup(rangeLdpConfig);
            return hybridLdp;
        }
        throw new IllegalArgumentException("Illegal RangeLdpConfig: " + rangeLdpConfig.getClass().getSimpleName());
    }
}

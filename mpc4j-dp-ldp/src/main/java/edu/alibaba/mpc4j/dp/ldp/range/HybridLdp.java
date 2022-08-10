package edu.alibaba.mpc4j.dp.ldp.range;

import edu.alibaba.mpc4j.dp.ldp.LdpConfig;

import java.util.Random;

/**
 * 混合范围LDP机制。由下述论文第III.C节描述：
 * <p>
 * Wang, Ning, Xiaokui Xiao, Yin Yang, Jun Zhao, Siu Cheung Hui, Hyejin Shin, Junbum Shin, and Ge Yu. Collecting
 * and analyzing multidimensional data with local differential privacy. ICDE 2019, pp. 638-649. IEEE, 2019.
 * </p>
 * <p>
 * In particular, given an input value ti, HM flips a coin whose head probability equals a constant α; if the coin
 * shows a head (resp. tail), then we invoke PM (resp. Duchi et al.’s solution) to perturb t_i.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/4/26
 */
class HybridLdp implements RangeLdp {
    /**
     * ε^*
     */
    private static final double EPSILON_STAR = Math.log(
        (-5 + 2 * Math.pow(6353 - 405 * Math.sqrt(241), 1.0 / 3)) / 27
            + (2 * Math.pow(6353 + 405 * Math.sqrt(241), 1.0 / 3)) / 27);
    /**
     * 配置项
     */
    private HybridLdpConfig hybridLdpConfig;
    /**
     * α
     */
    private double alpha;
    /**
     * 正面时，使用分段机制
     */
    private PiecewiseLdp piecewiseLdp;
    /**
     * 反面时，使用Minimax机制
     */
    private MinimaxLdp minimaxLdp;

    @Override
    public void setup(LdpConfig ldpConfig) {
        assert ldpConfig instanceof HybridLdpConfig;
        hybridLdpConfig = (HybridLdpConfig) ldpConfig;
        double epsilon = hybridLdpConfig.getBaseEpsilon();
        Random random = hybridLdpConfig.getRandom();
        if (epsilon > EPSILON_STAR) {
            // ε > ε^*, α = 1 - e^{-ε / 2}
            alpha = 1 - Math.exp(-epsilon / 2);
        } else {
            // ε <= ε^*, α = 0
            alpha = 0.0;
        }
        // 初始化分段机制
        PiecewiseLdpConfig piecewiseLdpConfig = new PiecewiseLdpConfig
            .Builder(epsilon)
            .setRandom(random)
            .build();
        piecewiseLdp = new PiecewiseLdp();
        piecewiseLdp.setup(piecewiseLdpConfig);
        // 初始化Minimax机制
        MinimaxLdpConfig minimaxLdpConfig = new MinimaxLdpConfig
            .Builder(epsilon)
            .setRandom(random)
            .build();
        minimaxLdp = new MinimaxLdp();
        minimaxLdp.setup(minimaxLdpConfig);
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        hybridLdpConfig.getRandom().setSeed(seed);
    }

    @Override
    public LdpConfig getLdpConfig() {
        return hybridLdpConfig;
    }

    @Override
    public double getEpsilon() {
        return hybridLdpConfig.getBaseEpsilon();
    }

    @Override
    public double randomize(double value) {
        assert value >= -1 && value <= 1 : "value must be in range [-1, 1]";
        // HM flips a coin whose head probability equals a constant α
        double coin = hybridLdpConfig.getRandom().nextDouble();
        if (coin < alpha) {
            // if the coin shows a head, then we invoke PM to perturb t_i.
            return piecewiseLdp.randomize(value);
        } else {
            // if the coin shows a tail, then we invoke Duchi et al.'s solution to perturb t_i.
            return minimaxLdp.randomize(value);
        }
    }
}

package edu.alibaba.mpc4j.dp.ldp.range;

import edu.alibaba.mpc4j.dp.ldp.LdpConfig;

import java.util.Random;

/**
 * 分段范围LDP机制。由下述论文第III.B节描述：
 * <p>
 * Wang, Ning, Xiaokui Xiao, Yin Yang, Jun Zhao, Siu Cheung Hui, Hyejin Shin, Junbum Shin, and Ge Yu. Collecting
 * and analyzing multidimensional data with local differential privacy. ICDE 2019, pp. 638-649. IEEE, 2019.
 * </p>
 * <p>
 * Our first proposal, referred to as the Piecewise Mechanism (PM), takes as input a value t_i ∈ [−1, 1], and outputs a
 * perturbed value t_i^* in [−C, C], where C = (e^{ε/2} + 1) / (e^{ε/2} - 1).
 * </p>
 * <p>
 * The probability density function (pdf) of t_i^* is a piecewise constant function as follows:
 * If x ∈ [l(t_i), r(t_i)], then pdf(t_i^* = x | t_i) = p;
 * If x ∈ [−C, l(t_i)) ∪ (r(t_i), C], then pdf(t_i^* = x | t_i) = p / e^ε;
 * </p>
 * <p>
 * p = (e^ε - e^{ε/2}) / (2e^{ε/2} + 2), l(t_i) = (C + 1) / 2 * t_i - (C - 1) / 2, r(t_i) = l(t_i) + C - 1.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/4/26
 */
public class PiecewiseLdp implements RangeLdp {
    /**
     * 配置项
     */
    private PiecewiseLdpConfig piecewiseLdpConfig;
    /**
     * C = (e^{ε/2} + 1) / (e^{ε/2} - 1)
     */
    private double c;
    /**
     * 门限值 = e^{ε/2} / (e^{ε/2} + 1)
     */
    private double threshold;

    @Override
    public void setup(LdpConfig ldpConfig) {
        assert ldpConfig instanceof PiecewiseLdpConfig;
        piecewiseLdpConfig = (PiecewiseLdpConfig) ldpConfig;
        double epsilon = piecewiseLdpConfig.getBaseEpsilon();
        c = (Math.exp(epsilon / 2) + 1) / (Math.exp(epsilon / 2) - 1);
        threshold = Math.exp(epsilon / 2) / (Math.exp(epsilon / 2) + 1);
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        piecewiseLdpConfig.getRandom().setSeed(seed);
    }

    @Override
    public LdpConfig getLdpConfig() {
        return piecewiseLdpConfig;
    }

    @Override
    public double getEpsilon() {
        return piecewiseLdpConfig.getBaseEpsilon();
    }

    @Override
    public double randomize(double value) {
        assert value >= -1 && value <= 1 : "value must be in range [-1, 1]";
        // l(t_i) = (C + 1) / 2 * t_i - (C - 1) / 2
        double l = (c + 1) / 2 * value - (c - 1) / 2;
        // r(t_i) = l(t_i) + C - 1
        double r = l + c - 1;
        // Sample x uniformly at random from [0, 1]
        Random random = piecewiseLdpConfig.getRandom();
        double x = random.nextDouble();
        if (x < threshold) {
            // if x < e^{ε/2} / (e^{ε/2} + 1), then Sample t_i^* uniformly at random from [l(t_i), r(t_i)]
            return l + random.nextDouble() * (r - l);
        } else {
            // else, Sample t_i^* uniformly at random from [−C, l(t_i)) ∪ (r(t_i), C]
            double leftRange = l + c;
            double rightRange = c - r;
            double range = leftRange + rightRange;
            double t = random.nextDouble() * range;
            if (t < leftRange) {
                return t - c;
            } else {
                return t - leftRange + r;
            }
        }
    }
}

package edu.alibaba.mpc4j.common.sampler.integral.nb;

import edu.alibaba.mpc4j.common.sampler.integral.poisson.ApachePoissonSampler;
import edu.alibaba.mpc4j.common.sampler.real.gamma.ApacheGammaSampler;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * 通过泊松分布变换得到的负二项分布采样。参考链接：
 * https://stats.stackexchange.com/questions/19031/how-to-draw-random-samples-from-a-negative-binomial-distribution-in-r
 *
 * Let 𝑋 have the Negative Binomial distribution with parameters r and p. The Negative Binomial distribution is a
 * mixture distribution or compound distribution. That is 𝑋 is Poisson(λ) where λ is randomly chosen from a
 * Gamma(r, p/(1 − p)).
 *
 * @author Weiran Liu
 * @date 2021/07/30
 */
public class ApacheNbSampler implements NbSampler {
    /**
     * 需要达到的成功次数
     */
    private final double r;
    /**
     * 每次实验成功的概率值
     */
    private final double p;
    /**
     * 依赖的Gamma分布
     */
    private final ApacheGammaSampler apacheGammaSampler;
    /**
     * 泊松分布要使用的随机数生成器
     */
    private final RandomGenerator randomGenerator;

    public ApacheNbSampler(double r, double p) {
        this(new JDKRandomGenerator(), r, p);
    }

    public ApacheNbSampler(RandomGenerator randomGenerator, double r, double p) {
        assert r > 0 : "r must be greater than 0";
        assert p >= 0 && p <= 1 : "p must be in range [0, 1]";
        this.r = r;
        this.p = p;
        this.randomGenerator = randomGenerator;
        apacheGammaSampler = new ApacheGammaSampler(randomGenerator, r, p / (1 - p));
    }

    @Override
    public int sample() {
        // λ is randomly chosen from a Gamma(r, p/(1 − p)).
        // 这里要防止λ的采样结果为0，如果为0则设置为精度允许的最小λ
        double lambda = Math.max(apacheGammaSampler.sample(), DoubleUtils.PRECISION);
        // 𝑋 is Poisson(λ)
        ApachePoissonSampler apachePoissonSampler = new ApachePoissonSampler(randomGenerator, lambda);

        return apachePoissonSampler.sample();
    }

    @Override
    public double getR() {
        return r;
    }

    @Override
    public double getP() {
        return p;
    }

    @Override
    public void reseed(long seed) {
        randomGenerator.setSeed(seed);
    }

    @Override
    public String toString() {
        return "(r = " + getR() + ", p = " + getP() + ")-" + getClass().getSimpleName();
    }
}

package edu.alibaba.mpc4j.common.sampler.integral.nb;

import edu.alibaba.mpc4j.common.sampler.integral.IntegralSampler;

/**
 * 负二项（Negative Binomial）分布采样接口。参考链接：https://en.wikipedia.org/wiki/Negative_binomial_distribution、
 *
 * Suppose there is a sequence of independent Bernoulli trials.
 * Each trial has two potential outcomes called "success" and "failure".
 * In each trial the probability of success is p and of failure is (1 − p).
 * We observe this sequence until a predefined number r of successes have occurred.
 * Then the random number of failures we have seen, X, will have the negative binomial (or Pascal) distribution
 * X ~ NP(r, p)。
 *
 * @author Weiran Liu
 * @date 2021/07/27
 */
public interface NbSampler extends IntegralSampler {
    /**
     * 返回负二项分布希望的成功次数r。
     *
     * @return 负二项分布希望的成功次数r。
     */
    double getR();

    /**
     * 返回负二项分布每次实验成功的概率p。
     *
     * @return 负二项分布每次实验成功的概率p。
     */
    double getP();

    @Override
    default double getMean() {
        // p * r / (1 - p)
        return getP() * getR() / (1.0 - getP());
    }

    @Override
    default double getVariance() {
        // p * r / (1 - p)^2
        return getP() * getR() / Math.pow(1.0 - getP(), 2);
    }
}

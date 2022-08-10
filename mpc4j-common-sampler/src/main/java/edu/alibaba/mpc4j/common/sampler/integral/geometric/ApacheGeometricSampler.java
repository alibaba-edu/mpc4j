package edu.alibaba.mpc4j.common.sampler.integral.geometric;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * 应用Apache的采样工具实现的双边几何分布采样。使用下述论文第2.4节给出的逆累积分布采样：
 * <p>
 * Balcer, Victor, and Salil Vadhan. Differential privacy on finite computers. arXiv preprint arXiv:1709.05396 (2017).
 * <p>
 * 论文中的s在本实现中用t表示，目的是与下述论文的符号描述相同：
 * Canonne, Clément L., Gautam Kamath, and Thomas Steinke. The discrete gaussian for differential privacy. Advances in
 * Neural Information Processing Systems 33 (2020): 15676-15688.
 *
 * @author Weiran Liu
 * @date 2022/4/9
 */
public class ApacheGeometricSampler implements GeometricSampler {
    /**
     * 随机数生成器
     */
    private final RandomGenerator randomGenerator;
    /**
     * 均值μ
     */
    private final int mu;
    /**
     * 放缩系数b
     */
    private final double b;
    /**
     * e^{1/b}
     */
    private final double expInverseB;

    public ApacheGeometricSampler(int mu, double b) {
        this(new JDKRandomGenerator(), mu, b);
    }

    public ApacheGeometricSampler(RandomGenerator randomGenerator, int mu, double b) {
        assert b > 0 : "b must be greater than 0";
        this.mu = mu;
        this.b = b;
        expInverseB = Math.exp(1.0 / b);
        this.randomGenerator = randomGenerator;
    }

    @Override
    public int getMu() {
        return mu;
    }

    @Override
    public double getB() {
        return b;
    }

    @Override
    public int sample() {
        double u = randomGenerator.nextDouble();
        // μ + ⌈b * sign(1/2 - u) * (ln(1 - |2u - 1|) + ln((e^{1/b} + 1) / 2))⌉ + ⌊2u⌋ - 1
        return mu
            + (int)Math.ceil(b * Math.signum(0.5 - u) * (Math.log(1.0 - Math.abs(2 * u - 1)) + Math.log((expInverseB + 1) / 2)))
            + (int)Math.floor(2 * u) - 1;
    }

    @Override
    public void reseed(long seed) {
        randomGenerator.setSeed(seed);
    }

    @Override
    public String toString() {
        return "(μ = " + getMu() + ", b = " + getB() + ")-" + getClass().getSimpleName();
    }
}

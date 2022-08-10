package edu.alibaba.mpc4j.common.sampler.real.laplace;

import com.google.privacy.differentialprivacy.LaplaceNoise;

/**
 * 谷歌Laplace采样器。
 *
 * @author Weiran Liu
 * @date 2022/03/25
 */
public class GoogleLaplaceSampler implements LaplaceSampler {
    /**
     * Laplace噪声
     */
    private final LaplaceNoise laplaceNoise;
    /**
     * 均值μ
     */
    private final double mu;
    /**
     * 放缩系数b
     */
    private final double b;
    /**
     * 1 / b
     */
    private final double bInverse;

    public GoogleLaplaceSampler(double mu, double b) {
        assert b > 0 : "b must be greater than 0";
        this.mu = mu;
        this.b = b;
        this.bInverse = 1.0 / b;
        laplaceNoise = new LaplaceNoise();
        // 尝试采样一次，保证参数是正确的
        sample();
    }

    @Override
    public double sample() {
        // 以Δf = 1.0，ε = 1.0 / b的参数采样
        return laplaceNoise.addNoise(0.0, 1.0, bInverse, null) + mu;
    }

    @Override
    public double getMu() {
        return mu;
    }

    @Override
    public double getB() {
        return b;
    }

    @Override
    public void reseed(long seed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "(μ = " + getMu() + ", b = " + getB() + ")-" + getClass().getSimpleName();
    }
}

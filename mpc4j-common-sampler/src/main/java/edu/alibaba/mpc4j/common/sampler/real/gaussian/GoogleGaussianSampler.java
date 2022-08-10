/*
 * Original Work Copyright 2020 Google LLC.
 * Modified Work Copyright 2022 Weiran Liu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package edu.alibaba.mpc4j.common.sampler.real.gaussian;

import edu.alibaba.mpc4j.common.sampler.SecureNoiseMath;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Google高斯采样器。由于无法直接调用Google库实现采样，因此这里外部调用。代码参见：
 * github.com/google/differential-privacy/blob/main/java/main/com/google/privacy/differentialprivacy/GaussianNoise.java
 *
 * @author Weiran Liu
 * @date 2022/03/25
 */
public class GoogleGaussianSampler implements GaussianSampler {
    /**
     * The square root of the maximum number n of Bernoulli trials from which a binomial sample is
     * drawn. Larger values result in more fine-grained noise, but increase the chance of sampling
     * inaccuracies due to overflows. The probability of such an event will be roughly 2^-45 or less,
     * if the square root is set to 2^57.
     */
    private static final double BINOMIAL_BOUND = (double) (1L << 57);
    /**
     * The absolute bound of the two sided geometric samples k that are used for creating a binomial
     * sample m + n / 2. For performance reasons, m is not composed of n Bernoulli trials. Instead m
     * is obtained via a rejection sampling technique, which sets m = (k + l) * (sqrt(2 * n) + 1),
     * where l is a uniform random sample between 0 and 1. Bounding k is therefore necessary to
     * prevent m from overflowing.
     *
     * <p>The probability of a single sample k being bounded is 2^-45. The overall privacy loss
     * resulting from this bound is minor and can safely be accounted for in the delta parameter.
     */
    private static final long GEOMETRIC_BOUND = (Long.MAX_VALUE / Math.round(Math.sqrt(2) * BINOMIAL_BOUND + 1.0)) - 1;
    /**
     * 随机数生成器
     */
    private final Random random;
    /**
     * 均值μ
     */
    private final double mu;
    /**
     * 标准差σ
     */
    private final double sigma;

    public GoogleGaussianSampler(double mu, double sigma) {
        assert sigma > 0 : "σ must be greater than 0";
        this.mu = mu;
        this.sigma = sigma;
        random = new SecureRandom();
    }

    @Override
    public double sample() {
        double granularity = getGranularity(sigma);
        // The square root of n is chosen in a way that places it in the interval between BINOMIAL_BOUND
        // and BINOMIAL_BOUND / 2. This ensures that the respective binomial distribution consists of
        // enough Bernoulli samples to closely approximate a Gaussian distribution.
        double sqrtN = 2.0 * sigma / granularity;
        long binomialSample = sampleSymmetricBinomial(sqrtN);
        return SecureNoiseMath.roundToMultipleOfPowerOfTwo(mu, granularity) + binomialSample * granularity;
    }

    @Override
    public double getMu() {
        return mu;
    }

    @Override
    public double getSigma() {
        return sigma;
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Determines the granularity of the output based on the sigma of the Gaussian noise.
     */
    private static double getGranularity(double sigma) {
        return SecureNoiseMath.ceilPowerOfTwo(2.0 * sigma / BINOMIAL_BOUND);
    }

    private long sampleSymmetricBinomial(double sqrtN) {
        assert sqrtN >= 1000000.0 : "Input must be at least 10^6. Provided value: " + sqrtN;
        assert Double.isFinite(sqrtN) : "Input must be finite. Provided value: " + sqrtN;

        long stepSize = Math.round(Math.sqrt(2) * sqrtN + 1.0);
        while (true) {
            long geometricSample = sampleBoundedGeometric();
            long twoSidedGeometricSample = random.nextBoolean() ? geometricSample : -geometricSample - 1;
            long result = stepSize * twoSidedGeometricSample + sampleUniform(stepSize);

            double resultProbability = approximateBinomialProbability(sqrtN, result);
            double rejectProbability = random.nextDouble();
            if (resultProbability > 0.0
                && rejectProbability > 0.0
                && rejectProbability
                < resultProbability * stepSize * Math.pow(2.0, geometricSample) / 4.0) {
                return result;
            }
        }
    }

    /**
     * Returns a sample drawn from the geometric distribution with success probability 1 / 2, i.e.,
     * the number of unsuccessful Bernoulli trials until the first success. The sample is capped
     * should it exceed the geometric bound.
     */
    private long sampleBoundedGeometric() {
        long result = 0;
        while (random.nextBoolean() && result < GEOMETRIC_BOUND) {
            result++;
        }
        return result;
    }

    /**
     * Draws an integer greater or equal to 0 and strictly less than {@code n} uniformly at random.
     * This custom implementation is necessary because SecureRandom provides such functionality only
     * for int but not for long.
     */
    private long sampleUniform(long n) {
        long largestMultipleOfN = (Long.MAX_VALUE / n) * n;

        while (true) {
            long signMask = 0x7fffffffffffffffL;
            long uniformNonNegativeLong = signMask & random.nextLong();
            if (uniformNonNegativeLong < largestMultipleOfN) {
                return uniformNonNegativeLong % n;
            }
        }
    }

    /**
     * Approximates the probability of a random sample {@code m + n / 2} drawn from a binomial
     * distribution of n Bernoulli trials that have a success probability of 1 / 2 each. The
     * approximation is taken from Lemma 7 of the noise generation documentation, available <a
     * href="https://github.com/google/differential-privacy/blob/main/common_docs/Secure_Noise_Generation.pdf">here</a>.
     *
     * <p>Note that m might be very large and m * m might not be representable as long.
     */
    private static double approximateBinomialProbability(double sqrtN, long m) {
        if (Math.abs(m) > sqrtN * Math.sqrt(Math.log(sqrtN) / 2)) {
            return 0.0;
        } else {
            return (Math.sqrt(2.0 / Math.PI) / sqrtN)
                * Math.exp(-2.0 * Math.pow(m / sqrtN, 2))
                * (1 - (0.4 * Math.pow(2.0 * Math.log(sqrtN), 1.5) / sqrtN));
        }
    }

    @Override
    public String toString() {
        return "(μ = " + getMu() + ", σ = " + getSigma() + ")-" + getClass().getSimpleName();
    }
}

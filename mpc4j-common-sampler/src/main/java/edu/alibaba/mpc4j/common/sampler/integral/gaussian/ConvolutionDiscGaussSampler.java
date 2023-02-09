package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import java.util.Arrays;
import java.util.Random;

/**
 * The convolution Discrete Gaussian sampler.
 * <p>
 * Applies the convolution technique to alias sampling in order to reduce memory overhead and setup cost at the cost of
 * running time. This is suitable for large $σ$. Any real-valued $c$ is accepted.
 * </p>
 * Modified from:
 * <p>
 * https://github.com/malb/dgs/blob/master/dgs/dgs_gauss_dp.c
 * </p>
 * The algorithm is described in the following papers:
 * <p>
 * Thomas Pöppelmann, Léo Ducas, Tim Güneysu. Enhanced Lattice-Based Signatures on Reconfigurable Hardware. CHES 2014,
 * pp 353-370, 2014.
 * </p>
 * and
 * <p>
 * Daniele Micciancio, Michael Walter. Gaussian Sampling over the Integers: Efficient, Generic, Constant-Time. CRYPTO
 * 2017, pp 455-485, 2017.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/28
 */
class ConvolutionDiscGaussSampler extends AbstractDiscGaussSampler {
    /**
     * coefficients for convolution
     */
    private final int[] coefficients;
    /**
     * base sampler for convolution
     */
    private final AliasTauDiscGaussSampler baseSampler;

    ConvolutionDiscGaussSampler(Random random, int c, double sigma) {
        super(random, c, sigma);
        double eta = 2;
        long tableSize = 2 * (long) Math.ceil(sigma * DiscGaussSamplerFactory.DEFAULT_TAU) * Integer.BYTES;
        int recursionLevel = 0;
        double currentSigma = sigma;
        int z1;
        int z2;
        // compute recursion level for convolution
        while (tableSize > DiscGaussSamplerFactory.MAX_TABLE_SIZE_BYTES) {
            recursionLevel++;
            z1 = (int) Math.floor(Math.sqrt(currentSigma / (eta * 2)));
            if (z1 == 0) {
                throw new IllegalStateException(
                    "MAX_TABLE_SIZE too small to store alias sampler: " + DiscGaussSamplerFactory.MAX_TABLE_SIZE_BYTES
                );
            }
            z2 = (z1 > 1) ? z1 - 1 : 1;
            currentSigma /= (Math.sqrt(z1 * z1 + z2 * z2));
            tableSize = 2 * (long) Math.ceil(currentSigma * DiscGaussSamplerFactory.DEFAULT_TAU) * Integer.BYTES;
        }
        coefficients = new int[1 << recursionLevel];
        Arrays.fill(coefficients, 1);
        // if there is no convolution, we simply forward to alias and we won't need adjustment of σ.
        currentSigma = sigma;
        // redo above computation to store coefficients
        for (int i = 0; i < recursionLevel; i++) {
            z1 = (int) Math.floor(Math.sqrt(currentSigma / (eta * 2)));
            z2 = (z1 > 1) ? z1 - 1 : 1;
            // we unroll the recursion on the coefficients on the fly
            // so we don't have to use recursion during the call
            int off = (1 << recursionLevel - i - 1);
            for (int j = 0; j < (1 << i); j++) {
                for (int k = 0; k < off; k++) {
                    coefficients[2 * j * off + k] *= z1;
                }
            }
            for (int j = 0; j < (1 << i); j++) {
                for (int k = 0; k < off; k++) {
                    coefficients[(2 * j + 1) * off + k] *= z2;
                }
            }
            currentSigma /= (Math.sqrt(z1 * z1 + z2 * z2));
        }
        baseSampler = new AliasTauDiscGaussSampler(random, 0, currentSigma, DiscGaussSamplerFactory.DEFAULT_TAU);
    }

    @Override
    public DiscGaussSamplerFactory.DiscGaussSamplerType getType() {
        return DiscGaussSamplerFactory.DiscGaussSamplerType.CONVOLUTION;
    }

    @Override
    public int sample() {
        int x = 0;
        for (int coefficient : coefficients) {
            x += coefficient * baseSampler.sample();
        }
        return x + c;
    }
}

package edu.alibaba.mpc4j.common.sampler.integral.gaussian;

import org.junit.Test;

/**
 * Convolution Discrete Gaussian Sampler test.
 * <p>
 * For the mean test, one would hope to test the convolution sampler with larger σ, but in that case the mean test is
 * likely to fail even if the sampler is correct. We still do some tests but this is mostly to ensure that the alias
 * sampler is instantiated correctly. Such tests are in the basic test.
 * </p>
 * <p>
 * For the σ tests, we need some large sigma test cases here, because everything else would just be retesting alias.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/28
 */
public class ConvolutionDiscGaussSamplerTest {

    @Test
    public void testRatios() {
        testRatios(150);
        testRatios(1500);
        testRatios(1 << 27);
    }

    private void testRatios(double sigma) {
        DiscGaussSampler sampler = DiscGaussSamplerFactory.createInstance(
            DiscGaussSamplerFactory.DiscGaussSamplerType.CONVOLUTION, 0, sigma
        );
        DiscGaussSamplerTestUtils.testRatios(sampler);

    }
}

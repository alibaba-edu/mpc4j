package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.crypto.fhe.rand.ClippedNormalDistribution;
import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGenerator;
import edu.alibaba.mpc4j.crypto.fhe.rand.UniformRandomGeneratorFactory;
import org.junit.Assert;
import org.junit.Test;

/**
 * Clip Normal unit tests.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/clipnormal.cpp
 * </p>
 *
 * @author Liqiang Peng
 * @date 2023/12/26
 */
public class ClipNormalTest {

    @Test
    public void testClipNormal() {
        UniformRandomGenerator rand = new UniformRandomGeneratorFactory().create();
        ClippedNormalDistribution dist = new ClippedNormalDistribution(50.0, 10.0, 20.0);

        Assert.assertEquals(0, Double.compare(50.0, dist.getMean()));
        Assert.assertEquals(0, Double.compare(10.0, dist.getStandardDeviation()));
        Assert.assertEquals(0, Double.compare(20.0, dist.getMaxDeviation()));
        double average = 0;
        double stddev = 0;
        for (int i = 0; i < 100; ++i) {
            double value = dist.sample(rand);
            average += value;
            stddev += (value - 50.0) * (value - 50.0);
            Assert.assertTrue(value >= 30.0 && value <= 70.0);
        }
        average /= 100;
        stddev /= 100;
        stddev = Math.sqrt(stddev);
        Assert.assertTrue(average >= 40.0 && average <= 60.0);
        Assert.assertTrue(stddev >= 5.0 && stddev <= 15.0);
    }
}

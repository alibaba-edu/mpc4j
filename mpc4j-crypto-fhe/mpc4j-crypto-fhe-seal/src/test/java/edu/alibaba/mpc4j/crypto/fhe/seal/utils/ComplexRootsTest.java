package edu.alibaba.mpc4j.crypto.fhe.seal.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

/**
 * Complex Roots test. The expect results are computed using C/C++ version of SEAL.
 *
 * @author Weiran Liu
 * @date 2025/2/14
 */
public class ComplexRootsTest {

    @Test
    public void testRootDegree8() {
        // we test more roots since the implementation automatically do mod operations.
        double[][] expect = new double[][]{
            new double[]{1, 0},
            new double[]{0.7071067811865476, 0.7071067811865475},
            new double[]{0, 1},
            new double[]{-0.7071067811865476, 0.7071067811865475},
            new double[]{-1, 0},
            new double[]{-0.7071067811865476, -0.7071067811865475},
            new double[]{0, -1},
            new double[]{0.7071067811865476, -0.7071067811865475},
        };
        testRootDegree(expect);
    }

    @Test
    public void testRootDegree16() {
        double[][] expect = new double[][]{
            new double[]{1, 0},
            new double[]{0.9238795325112867, 0.3826834323650898},
            new double[]{0.7071067811865476, 0.7071067811865475},
            new double[]{0.3826834323650898, 0.9238795325112867},
            new double[]{0, 1},
            new double[]{-0.3826834323650898, 0.9238795325112867},
            new double[]{-0.7071067811865476, 0.7071067811865475},
            new double[]{-0.9238795325112867, 0.3826834323650898},
            new double[]{-1, 0},
            new double[]{-0.9238795325112867, -0.3826834323650898},
            new double[]{-0.7071067811865476, -0.7071067811865475},
            new double[]{-0.3826834323650898, -0.9238795325112867},
            new double[]{-0, -1},
            new double[]{0.3826834323650898, -0.9238795325112867},
            new double[]{0.7071067811865476, -0.7071067811865475},
            new double[]{0.9238795325112867, -0.3826834323650898},
        };
        testRootDegree(expect);
    }

    @Test
    public void testRootDegree32() {
        double[][] expect = new double[][]{
            new double[]{1, 0},
            new double[]{0.9807852804032304, 0.1950903220161282},
            new double[]{0.9238795325112867, 0.3826834323650898},
            new double[]{0.8314696123025452, 0.5555702330196022},
            new double[]{0.7071067811865476, 0.7071067811865475},
            new double[]{0.5555702330196022, 0.8314696123025452},
            new double[]{0.3826834323650898, 0.9238795325112867},
            new double[]{0.1950903220161282, 0.9807852804032304},
            new double[]{0, 1},
            new double[]{-0.1950903220161282, 0.9807852804032304},
            new double[]{-0.3826834323650898, 0.9238795325112867},
            new double[]{-0.5555702330196022, 0.8314696123025452},
            new double[]{-0.7071067811865476, 0.7071067811865475},
            new double[]{-0.8314696123025452, 0.5555702330196022},
            new double[]{-0.9238795325112867, 0.3826834323650898},
            new double[]{-0.9807852804032304, 0.1950903220161282},
            new double[]{-1, 0},
            new double[]{-0.9807852804032304, -0.1950903220161282},
            new double[]{-0.9238795325112867, -0.3826834323650898},
            new double[]{-0.8314696123025452, -0.5555702330196022},
            new double[]{-0.7071067811865476, -0.7071067811865475},
            new double[]{-0.5555702330196022, -0.8314696123025452},
            new double[]{-0.3826834323650898, -0.9238795325112867},
            new double[]{-0.1950903220161282, -0.9807852804032304},
            new double[]{-0, -1},
            new double[]{0.1950903220161282, -0.9807852804032304},
            new double[]{0.3826834323650898, -0.9238795325112867},
            new double[]{0.5555702330196022, -0.8314696123025452},
            new double[]{0.7071067811865476, -0.7071067811865475},
            new double[]{0.8314696123025452, -0.5555702330196022},
            new double[]{0.9238795325112867, -0.3826834323650898},
            new double[]{0.9807852804032304, -0.1950903220161282},
        };
        testRootDegree(expect);
    }

    private void testRootDegree(double[][] expect) {
        int rootDegree = expect.length;
        ComplexRoots complexRoots = new ComplexRoots(rootDegree);
        double[][] actual = IntStream.range(0, rootDegree)
            .mapToObj(complexRoots::get_root)
            .toArray(double[][]::new);
        Assert.assertEquals(expect.length, actual.length);
        for (int i = 0; i < expect.length; i++) {
            Assert.assertArrayEquals(expect[i], actual[i], 1e-16);
        }
    }
}

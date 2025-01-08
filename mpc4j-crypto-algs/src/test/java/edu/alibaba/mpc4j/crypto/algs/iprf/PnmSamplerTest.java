package edu.alibaba.mpc4j.crypto.algs.iprf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.junit.Assert;
import org.junit.Test;

import java.nio.IntBuffer;
import java.security.SecureRandom;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Pseudorandom Multinomial Sampler (PMNS) test.
 *
 * @author Weiran Liu
 * @date 2024/8/23
 */
public class PnmSamplerTest {
    /**
     * parallel num
     */
    private static final int PARALLEL_NUM = 1 << 10;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public PnmSamplerTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testIllegalInputs() {
        PnmSampler sampler = new PnmSampler(EnvType.STANDARD);
        int n = 20;
        int m = 10;
        // set short key
        Assert.assertThrows(AssertionError.class, () -> sampler.init(n, m, new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1]));
        // set long key
        Assert.assertThrows(AssertionError.class, () -> sampler.init(n, m, new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1]));

        byte[] key = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        // set negative n
        Assert.assertThrows(IllegalArgumentException.class, () -> sampler.init(-1, m, key));
        // set zero n
        Assert.assertThrows(IllegalArgumentException.class, () -> sampler.init(0, m, key));
        // set negative m
        Assert.assertThrows(IllegalArgumentException.class, () -> sampler.init(n, -1, key));
        // set zero m
        Assert.assertThrows(IllegalArgumentException.class, () -> sampler.init(n, 0, key));
        // execute without init
        Assert.assertThrows(IllegalArgumentException.class, () -> sampler.sample(5));
        Assert.assertThrows(IllegalArgumentException.class, () -> sampler.inverseSample(5));

        sampler.init(n, m, key);
        // negative x and y
        Assert.assertThrows(IllegalArgumentException.class, () -> sampler.sample(-1));
        Assert.assertThrows(IllegalArgumentException.class, () -> sampler.inverseSample(-1));
        // x = n or y = m
        Assert.assertThrows(IllegalArgumentException.class, () -> sampler.sample(n));
        Assert.assertThrows(IllegalArgumentException.class, () -> sampler.inverseSample(m));
        // large x or large y
        Assert.assertThrows(IllegalArgumentException.class, () -> sampler.sample(n + 1));
        Assert.assertThrows(IllegalArgumentException.class, () -> sampler.inverseSample(m + 1));
    }

    @Test
    public void testConstant() {
        for (int n = 1; n <= 20; n++) {
            for (int m = 1; m <= 10; m++) {
                testConstant(n, m);
            }
        }
    }

    private void testConstant(int n, int m) {
        PnmSampler sampler = new PnmSampler(EnvType.STANDARD);
        byte[] key = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        sampler.init(n, m, key);
        // sample
        TIntList[] bins = IntStream.range(0, m)
            .mapToObj(y -> new TIntArrayList())
            .toArray(TIntList[]::new);
        for (int x = 0; x < n; x++) {
            int y = sampler.sample(x);
            bins[y].add(x);
        }
        int[][] samples = IntStream.range(0, m)
            .mapToObj(y -> bins[y].toArray())
            .toArray(int[][]::new);
        // inverse sample
        for (int y = 0; y < m; y++) {
            int[] xs = sampler.inverseSample(y);
            Assert.assertArrayEquals(samples[y], xs);
        }
    }

    @Test
    public void testParallel() {
        PnmSampler sampler = new PnmSampler(EnvType.STANDARD);
        byte[] key = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        sampler.init(20, 10, key);
        // sample
        Set<Integer> ciphertextSet = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .map(index -> sampler.sample(0))
            .boxed()
            .collect(Collectors.toSet());
        Assert.assertEquals(1, ciphertextSet.size());
        // inverse PRP
        Set<IntBuffer> plaintextSet = IntStream.range(0, PARALLEL_NUM)
            .parallel()
            .mapToObj(index -> sampler.inverseSample(0))
            .map(IntBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, plaintextSet.size());
    }
}

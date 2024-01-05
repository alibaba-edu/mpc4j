package edu.alibaba.mpc4j.crypto.fhe.rand;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.crypto.fhe.serialization.ComprModeType;
import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * UniformRandomGenerator test.
 * <p></p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/randomgen.cpp.
 *
 * @author Weiran Liu
 * @date 2023/11/29
 */
@RunWith(Parameterized.class)
public class UniformRandomGeneratorTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configuration = new ArrayList<>();

        // SHA1PRNG
        configuration.add(new Object[]{PrngType.SHA1PRNG.name(), PrngType.SHA1PRNG,});

        return configuration;
    }

    public UniformRandomGeneratorTest(String name, PrngType prngType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.prngType = prngType;
    }

    /**
     * the PRNG type
     */
    private final PrngType prngType;

    @Test
    public void testUniformRandomCreateDefault() {
        UniformRandomGeneratorFactory factory = UniformRandomGeneratorFactory.defaultFactory();
        Assert.assertTrue(factory.useRandomSeed());
        UniformRandomGenerator randomGenerator = factory.create(prngType);
        boolean lowerHalf = false;
        boolean upperHalf = false;
        boolean even = false;
        boolean odd = false;
        // generate 20 random values and see if there is at least one lower_half, one upper_half, one even and one odd
        for (int i = 0; i < 20; i++) {
            int value = randomGenerator.nextInt();
            if (value < Integer.MAX_VALUE / 2) {
                lowerHalf = true;
            } else {
                upperHalf = true;
            }
            if ((value % 2) == 0) {
                even = true;
            } else {
                odd = true;
            }
        }
        Assert.assertTrue(lowerHalf);
        Assert.assertTrue(upperHalf);
        Assert.assertTrue(even);
        Assert.assertTrue(odd);
    }

    @Test
    public void testRandomGeneratorFactorySeed() {
        UniformRandomGeneratorFactory factory = UniformRandomGeneratorFactory.defaultFactory();
        Assert.assertTrue(factory.useRandomSeed());
        Assert.assertNull(factory.defaultSeed());

        factory = new UniformRandomGeneratorFactory(new long[] {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L});
        Assert.assertFalse(factory.useRandomSeed());
        Assert.assertArrayEquals(new long[] {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L}, factory.defaultSeed());

        factory = UniformRandomGeneratorFactory.defaultFactory();
        Assert.assertTrue(factory.useRandomSeed());
    }

    @Test
    public void testRandomUint64() {
        UniformRandomGenerator generator = UniformRandomGeneratorFactory.defaultFactory().create(prngType);
        TLongSet values = new TLongHashSet();
        int count = 100;
        for (int i = 0; i < count; i++) {
            values.add(generator.nextLong());
        }
        Assert.assertEquals(count, values.size());
    }

    @Test
    public void testRandomSeededRng() {
        UniformRandomGenerator generator1 = UniformRandomGeneratorFactory.defaultFactory().create(prngType);
        int[] values1 = IntStream.range(0, 20).map(index -> generator1.nextInt()).toArray();

        UniformRandomGenerator generator2 = UniformRandomGeneratorFactory.defaultFactory()
            .create(prngType, new long[Common.BYTES_PER_UINT64]);
        int[] values2 = IntStream.range(0, 20).map(index -> generator2.nextInt()).toArray();

        UniformRandomGenerator generator3 = UniformRandomGeneratorFactory.defaultFactory()
            .create(prngType, new long[Common.BYTES_PER_UINT64]);
        int[] values3 = IntStream.range(0, 20).map(index -> generator3.nextInt()).toArray();

        for (int i = 0; i < 20; i++) {
            Assert.assertNotEquals(values1[i], values2[i]);
            Assert.assertEquals(values2[i], values3[i]);
        }

        int val1, val2, val3;
        val1 = generator1.nextInt();
        val2 = generator2.nextInt();
        val3 = generator3.nextInt();
        Assert.assertNotEquals(val1, val2);
        Assert.assertEquals(val2, val3);
    }

    @Test
    public void testUniformRandomGeneratorInfo() {
        UniformRandomGeneratorInfo info = new UniformRandomGeneratorInfo();
        Assert.assertEquals(PrngType.UNKNOWN, info.getType());
        Assert.assertTrue(info.hasValidPrngType());

        long[] seedArr = new long[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        UniformRandomGenerator rg = UniformRandomGeneratorFactory.defaultFactory().create(prngType, seedArr);
        info = rg.getInfo();

        Assert.assertEquals(PrngType.SHA1PRNG, info.getType());
        Assert.assertTrue(info.hasValidPrngType());
        Assert.assertArrayEquals(seedArr, info.getSeed());

        UniformRandomGenerator rg2 = info.makePrng();
        Assert.assertNotNull(rg2);
        for (int i = 0; i < 100; i++) {
            Assert.assertEquals(rg.nextLong(), rg2.nextLong());
        }
    }

    @Test
    public void testUniformRandomGeneratorInfoSaveLoad() throws IOException {
        UniformRandomGeneratorInfo info = new UniformRandomGeneratorInfo();
        UniformRandomGeneratorInfo info2 = new UniformRandomGeneratorInfo();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        info.save(outputStream, ComprModeType.NONE);
        info2.load(null, new ByteArrayInputStream(outputStream.toByteArray()));
        Assert.assertEquals(info, info2);
        outputStream.reset();

        long[] seedArr = new long[] { 1, 2, 3, 4, 5, 6, 7, 8 };
        UniformRandomGenerator rg = UniformRandomGeneratorFactory.defaultFactory().create(prngType, seedArr);
        info = rg.getInfo();
        info.save(outputStream);
        info2.load(null, new ByteArrayInputStream(outputStream.toByteArray()));
        Assert.assertEquals(info, info2);
    }
}

package edu.alibaba.mpc4j.common.tool.hash;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hash.LongHashFactory.LongHashType;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * tests for long hash.
 *
 * @author Weiran Liu
 * @date 2023/1/4
 */
@RunWith(Parameterized.class)
public class LongHashTest {
    /**
     * test round
     */
    private static final int TEST_ROUND = 1 << CommonConstants.STATS_BYTE_LENGTH;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // XXHash
        configurations.add(new Object[]{LongHashType.XX_HASH_64.name(), LongHashType.XX_HASH_64});
        // BobHash
        configurations.add(new Object[]{LongHashType.BOB_HASH_64.name(), LongHashType.BOB_HASH_64});

        return configurations;
    }

    /**
     * the type
     */
    private final LongHashType type;

    public LongHashTest(String name, LongHashType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testIllegalInputs() {
        LongHash longHash = LongHashFactory.createInstance(type);
        // try input data with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> longHash.hash(new byte[0]));
        Assert.assertThrows(IllegalArgumentException.class, () -> longHash.hash(new byte[0], 0L));
    }

    @Test
    public void testType() {
        LongHash longHash = LongHashFactory.createInstance(type);
        Assert.assertEquals(type, longHash.getType());
    }

    @Test
    public void testHash() {
        LongHash longHash = LongHashFactory.createInstance(type);
        // different inputs should return different hash results.
        Set<Long> hashSet = IntStream.range(0, TEST_ROUND)
            .mapToLong(index -> longHash.hash(IntUtils.intToByteArray(index)))
            .boxed()
            .collect(Collectors.toSet());
        Assert.assertEquals(TEST_ROUND, hashSet.size());
    }

    @Test
    public void testSeedHash() {
        byte[] data = new byte[CommonConstants.STATS_BYTE_LENGTH];
        LongHash longHash = LongHashFactory.createInstance(type);
        // different seeds with the same input should return different hash results.
        Set<Long> hashSet = IntStream.range(0, TEST_ROUND)
            .mapToLong(seed -> longHash.hash(data, seed))
            .boxed()
            .collect(Collectors.toSet());
        Assert.assertEquals(TEST_ROUND, hashSet.size());
    }

    @Test
    public void testParallel() {
        byte[] data = new byte[CommonConstants.STATS_BYTE_LENGTH];
        LongHash longHash = LongHashFactory.createInstance(type);
        Set<Long> hashSet = IntStream.range(0, TEST_ROUND)
            .parallel()
            .mapToLong(index -> longHash.hash(data))
            .boxed()
            .collect(Collectors.toSet());
        Assert.assertEquals(1, hashSet.size());
    }
}

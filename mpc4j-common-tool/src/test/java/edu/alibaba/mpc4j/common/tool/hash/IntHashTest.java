package edu.alibaba.mpc4j.common.tool.hash;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory.IntHashType;
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
 * tests for int hash.
 *
 * @author Weiran Liu
 * @date 2023/1/4
 */
@RunWith(Parameterized.class)
public class IntHashTest {
    /**
     * test round
     */
    private static final int TEST_ROUND = 1 << CommonConstants.STATS_BYTE_LENGTH;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // XXHash
        configurations.add(new Object[]{IntHashType.XX_HASH_32.name(), IntHashType.XX_HASH_32});
        // BobHash
        configurations.add(new Object[]{IntHashType.BOB_HASH_32.name(), IntHashType.BOB_HASH_32});

        return configurations;
    }

    /**
     * the type
     */
    private final IntHashType type;

    public IntHashTest(String name, IntHashType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testIllegalInputs() {
        IntHash intHash = IntHashFactory.createInstance(type);
        // try input data with 0 length
        Assert.assertThrows(IllegalArgumentException.class, () -> intHash.hash(new byte[0]));
        Assert.assertThrows(IllegalArgumentException.class, () -> intHash.hash(new byte[0], 0));
    }

    @Test
    public void testType() {
        IntHash intHash = IntHashFactory.createInstance(type);
        Assert.assertEquals(type, intHash.getType());
    }

    @Test
    public void testHash() {
        IntHash intHash = IntHashFactory.createInstance(type);
        // different inputs should return different hash results.
        Set<Integer> hashSet = IntStream.range(0, TEST_ROUND)
            .map(index -> intHash.hash(IntUtils.intToByteArray(index)))
            .boxed()
            .collect(Collectors.toSet());
        Assert.assertEquals(TEST_ROUND, hashSet.size());
    }

    @Test
    public void testSeedHash() {
        byte[] data = new byte[CommonConstants.STATS_BYTE_LENGTH];
        IntHash intHash = IntHashFactory.createInstance(type);
        // different seeds with the same input should return different hash results.
        Set<Integer> hashSet = IntStream.range(0, TEST_ROUND)
            .map(seed -> intHash.hash(data, seed))
            .boxed()
            .collect(Collectors.toSet());
        Assert.assertEquals(TEST_ROUND, hashSet.size());
    }

    @Test
    public void testParallel() {
        byte[] data = new byte[CommonConstants.STATS_BYTE_LENGTH];
        IntHash intHash = IntHashFactory.createInstance(type);
        Set<Integer> hashSet = IntStream.range(0, TEST_ROUND)
            .parallel()
            .map(index -> intHash.hash(data))
            .boxed()
            .collect(Collectors.toSet());
        Assert.assertEquals(1, hashSet.size());
    }
}

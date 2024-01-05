package edu.alibaba.mpc4j.common.structure.filter;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * distinct filter test.
 *
 * @author Weiran Liu
 * @date 2023/7/10
 */
@RunWith(Parameterized.class)
public class DistinctBloomFilterTest {
    /**
     * max random round
     */
    private static final int MAX_RANDOM_ROUND = 1 << 16;
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = 1 << 8;
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();

        // DISTINCT_BLOOM_FILTER
        configurationParams.add(new Object[]{FilterType.DISTINCT_BLOOM_FILTER.name(), FilterType.DISTINCT_BLOOM_FILTER,});

        return configurationParams;
    }

    private final FilterType type;

    public DistinctBloomFilterTest(String name, FilterType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testDistinctFilter() {
        byte[] keys = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(keys);
        BloomFilter<ByteBuffer> filter = FilterFactory.createBloomFilter(EnvType.STANDARD, type, DEFAULT_SIZE, keys);
        byte[] itemByteArray = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            // generate random inputs and test if the hash index are distinct
            SECURE_RANDOM.nextBytes(itemByteArray);
            ByteBuffer item = ByteBuffer.wrap(itemByteArray);
            int[] hashIndexes = filter.hashIndexes(item);
            long distinctIndexes = Arrays.stream(hashIndexes).distinct().count();
            Assert.assertEquals(hashIndexes.length, distinctIndexes);
        }
    }
}

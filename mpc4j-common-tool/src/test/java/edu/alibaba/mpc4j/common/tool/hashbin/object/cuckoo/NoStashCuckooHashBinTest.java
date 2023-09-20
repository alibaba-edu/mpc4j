package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * no-stash cuckoo hash bin test.
 *
 * @author Weiran Liu
 * @date 2023/8/2
 */
@Ignore
@RunWith(Parameterized.class)
public class NoStashCuckooHashBinTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoStashCuckooHashBinTest.class);
    /**
     * test round
     */
    private static final int TEST_ROUND = 1 << 10;
    /**
     * max small item size
     */
    private static final int MAX_SMALL_ITEM_SIZE = 256;
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // d = 5
        configurations.add(new Object[]{
            "d = 5", CuckooHashBinType.NO_STASH_PSZ18_5_HASH
        });
        // d = 4
        configurations.add(new Object[]{
            "d = 4", CuckooHashBinType.NO_STASH_PSZ18_4_HASH
        });
        // d = 3
        configurations.add(new Object[]{
            "d = 3", CuckooHashBinType.NO_STASH_PSZ18_3_HASH
        });

        return configurations;
    }

    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType type;
    /**
     * hash num
     */
    private final int hashNum;

    public NoStashCuckooHashBinTest(String name, CuckooHashBinType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        hashNum = CuckooHashBinFactory.getHashNum(type);
    }

    @Test
    public void testSmallItemSize() {
        for (int itemSize = 1; itemSize <= MAX_SMALL_ITEM_SIZE; itemSize++) {
            // generate small items
            List<ByteBuffer> items = IntStream.range(0, itemSize)
                .mapToObj(IntUtils::intToByteArray)
                .map(ByteBuffer::wrap)
                .collect(Collectors.toList());
            int finalItemSize = itemSize;
            int noStashCount = IntStream.range(0, TEST_ROUND).parallel()
                .map(round -> {
                    try {
                        // we try TEST_ROUND to see if all insertion results are no-stash
                        byte[][] keys = CommonUtils.generateRandomKeys(hashNum, SECURE_RANDOM);
                        NoStashCuckooHashBin<ByteBuffer> noStashCuckooHashBin = CuckooHashBinFactory
                            .createNoStashCuckooHashBin(EnvType.STANDARD, type, finalItemSize, keys);
                        noStashCuckooHashBin.insertItems(items);
                        return 1;
                    } catch (ArithmeticException e) {
                        return 0;
                    }
                })
                .sum();
            if (TEST_ROUND != noStashCount) {
                LOGGER.info("We meet a stashed case: itemSize = {}", itemSize);
            }
            Assert.assertTrue(noStashCount >= TEST_ROUND - 1);
        }
    }
}

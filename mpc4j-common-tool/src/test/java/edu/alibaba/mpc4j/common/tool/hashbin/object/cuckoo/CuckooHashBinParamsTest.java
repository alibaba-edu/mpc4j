package edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Cuckoo hash bin parameters test. Here we text statistical security parameters for small bin num.
 *
 * @author Weiran Liu
 * @date 2023/7/27
 */
@Ignore
@RunWith(Parameterized.class)
public class CuckooHashBinParamsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CuckooHashBinParamsTest.class);
    /**
     * max round
     */
    private static final int MAX_ROUND = 1 << 22;
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * log(num) -> bins for d = 3
     */
    private static final TIntObjectMap<int[]> H3_LOG_NUM_BINS_MAP = new TIntObjectHashMap<>(7);

    static {
        H3_LOG_NUM_BINS_MAP.put(1, new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,});
        H3_LOG_NUM_BINS_MAP.put(2, new int[]{7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,});
        H3_LOG_NUM_BINS_MAP.put(3, new int[]{12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,});
        H3_LOG_NUM_BINS_MAP.put(4, new int[]{21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32,});
        H3_LOG_NUM_BINS_MAP.put(5, new int[]{39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50,});
        H3_LOG_NUM_BINS_MAP.put(6, new int[]{71, 73, 75, 77, 79, 81, 83, 85, 87, 89, 90, 92,});
        H3_LOG_NUM_BINS_MAP.put(7, new int[]{155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166,});
    }

    /**
     * log(num) -> bins for d = 4
     */
    private static final TIntObjectMap<int[]> H4_LOG_NUM_BINS_MAP = new TIntObjectHashMap<>(7);

    static {
        H4_LOG_NUM_BINS_MAP.put(1, new int[]{3, 4, 5, 6, 7, 8,});
        H4_LOG_NUM_BINS_MAP.put(2, new int[]{5, 6, 7, 8, 9, 10,});
        H4_LOG_NUM_BINS_MAP.put(3, new int[]{9, 10, 11, 12, 13, 14,});
        H4_LOG_NUM_BINS_MAP.put(4, new int[]{17, 18, 19, 20, 21, 22, 23,});
        H4_LOG_NUM_BINS_MAP.put(5, new int[]{33, 34, 35, 36, 37, 38, 39, 40,});
        H4_LOG_NUM_BINS_MAP.put(6, new int[]{68, 69, 70, 71, 72, 73, 74, 75,});
        H4_LOG_NUM_BINS_MAP.put(7, new int[]{135, 136, 137, 138, 139, 140, 141, 142,});
    }

    /**
     * log(num) -> bins for d = 5
     */
    private static final TIntObjectMap<int[]> H5_LOG_NUM_BINS_MAP = new TIntObjectHashMap<>(7);

    static {
        H5_LOG_NUM_BINS_MAP.put(1, new int[]{3, 4, 5,});
        H5_LOG_NUM_BINS_MAP.put(2, new int[]{5, 6, 7,});
        H5_LOG_NUM_BINS_MAP.put(3, new int[]{9, 10, 11,});
        H5_LOG_NUM_BINS_MAP.put(4, new int[]{17, 18, 19, 20,});
        H5_LOG_NUM_BINS_MAP.put(5, new int[]{33, 34, 35, 36,});
        H5_LOG_NUM_BINS_MAP.put(6, new int[]{65, 66, 67, 68, 69, 70,});
        H5_LOG_NUM_BINS_MAP.put(7, new int[]{130, 131, 132, 133, 134, 135, 136,});
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // d = 3
        configurations.add(new Object[]{
            "d = 3", CuckooHashBinType.NO_STASH_PSZ18_3_HASH
        });
        // d = 4
        configurations.add(new Object[]{
            "d = 4", CuckooHashBinType.NO_STASH_PSZ18_4_HASH
        });
        // d = 5
        configurations.add(new Object[]{
            "d = 5", CuckooHashBinType.NO_STASH_PSZ18_5_HASH
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

    public CuckooHashBinParamsTest(String name, CuckooHashBinType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        hashNum = CuckooHashBinFactory.getHashNum(type);
    }

    @Test
    public void testLogNum1() {
        testLogNum(1);
    }

    @Test
    public void testLogNum2() {
        testLogNum(2);
    }

    @Test
    public void testLogNum3() {
        testLogNum(3);
    }

    @Test
    public void testLogNum4() {
        testLogNum(4);
    }

    @Test
    public void testLogNum5() {
        testLogNum(5);
    }

    @Test
    public void testLogNum6() {
        testLogNum(6);
    }

    @Test
    public void testLogNum7() {
        testLogNum(7);
    }

    private void testLogNum(int logNum) {
        int[] binNumArray;
        switch (hashNum) {
            case 3:
                binNumArray = H3_LOG_NUM_BINS_MAP.get(logNum);
                break;
            case 4:
                binNumArray = H4_LOG_NUM_BINS_MAP.get(logNum);
                break;
            case 5:
                binNumArray = H5_LOG_NUM_BINS_MAP.get(logNum);
                break;
            default:
                throw new IllegalArgumentException("Invalid hash num: " + hashNum);
        }
        int num = 1 << logNum;
        List<ByteBuffer> items = IntStream.range(0, num)
            .mapToObj(IntUtils::intToByteArray)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toList());
        LOGGER.info("log(num) = {}, bin num = {}", logNum, Arrays.toString(binNumArray));
        for (int binNum : binNumArray) {
            // for each bin num, test its security parameter
            int noStashCount = IntStream.range(0, MAX_ROUND).parallel()
                .map(round -> {
                    // try to insert items and see if it is no stash
                    try {
                        byte[][] keys = CommonUtils.generateRandomKeys(CuckooHashBinFactory.getHashNum(type), SECURE_RANDOM);
                        NoStashCuckooHashBin<ByteBuffer> hashBin = CuckooHashBinFactory.createNoStashCuckooHashBin(
                            EnvType.STANDARD, type, num, binNum, keys
                        );
                        hashBin.insertItems(items);
                        return 1;
                    } catch (ArithmeticException e) {
                        return 0;
                    }
                })
                .sum();
            double sigma = -1 * DoubleUtils.log2(1 - (double) noStashCount / MAX_ROUND);
            LOGGER.info("log(num) = {}, bin = {}, σ = {}", logNum, binNum, sigma);
        }
    }
}

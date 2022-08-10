package edu.alibaba.mpc4j.common.tool.hashbin;

import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 哈希桶测试工具类。
 *
 * @author Weiran Liu
 * @date 2022/02/23
 */
public class HashBinTestUtils {
    /**
     * 随机状态
     */
    public static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 私有构造函数。
     */
    private HashBinTestUtils() {
        // empty
    }

    /**
     * 返回随机元素列表。
     *
     * @param size 元素数量。
     * @return 随机元素列表。
     */
    public static List<ByteBuffer> randomByteBufferItems(int size) {
        return IntStream.range(0, size)
            .mapToObj(index -> {
                byte[] itemByteArray = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                SECURE_RANDOM.nextBytes(itemByteArray);
                return ByteBuffer.wrap(itemByteArray);
            })
            .collect(Collectors.toList());
    }

    /**
     * 返回随机元素列表。
     *
     * @param size 元素数量。
     * @return 随机元素列表。
     */
    public static List<BigInteger> randomBigIntegerItems(int size) {
        return IntStream.range(0, size)
            .mapToObj(index -> new BigInteger(CommonConstants.BLOCK_BIT_LENGTH, SECURE_RANDOM))
            .collect(Collectors.toList());
    }

    /**
     * 返回随机元素列表，每个数据一定为非负数。
     *
     * @param size 元素数量。
     * @return 随机元素列表。
     */
    public static int[] randomIntItems(int size) {
        Set<Integer> itemSet = new HashSet<>(size);
        while (itemSet.size() < size) {
            itemSet.add(Math.abs(HashBinTestUtils.SECURE_RANDOM.nextInt()));
        }
        return itemSet.stream().mapToInt(item -> item).toArray();
    }
}

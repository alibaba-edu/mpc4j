package edu.alibaba.mpc4j.common.structure.fusefilter;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Weiran Liu
 * @date 2024/7/25
 */
public class ByteFuseFilterTest {
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = 1 << 10 + 1;
    /**
     * default value byte length
     */
    private static final int DEFAULT_VALUE_BYTE_LENGTH = 16;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public ByteFuseFilterTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testDefault() {
        testByteFuseFilter(DEFAULT_SIZE, DEFAULT_VALUE_BYTE_LENGTH);
    }

    @Test
    public void testSmallSize() {
        for (int size = 1; size < 40; size++) {
            testByteFuseFilter(size, DEFAULT_VALUE_BYTE_LENGTH);
        }
    }

    @Test
    public void testSmallValueByteLength() {
        for (int valueByteLength = 1; valueByteLength < DEFAULT_VALUE_BYTE_LENGTH; valueByteLength++) {
            testByteFuseFilter(DEFAULT_SIZE, valueByteLength);
        }
    }

    @Test
    public void testLogSetSize() {
        for (int logSize = 0; logSize < 22; logSize++) {
            testByteFuseFilter(1 << logSize, DEFAULT_VALUE_BYTE_LENGTH);
        }
    }

    private void testByteFuseFilter(int size, int valueByteLength) {
        Map<ByteBuffer, byte[]> keyValueMap = randomKeyValueMap(size, valueByteLength);
        Arity3ByteFuseFilter<ByteBuffer> byteFuseFilter = new Arity3ByteFuseFilter<>(
            EnvType.STANDARD, keyValueMap, valueByteLength, secureRandom
        );
        byte[] seed = byteFuseFilter.seed();
        byte[][] storage = byteFuseFilter.storage();
        Arity3ByteFusePosition<ByteBuffer> byteFusePosition = new Arity3ByteFusePosition<>(
            EnvType.STANDARD, size, valueByteLength, seed
        );
        // verify
        for (ByteBuffer key : keyValueMap.keySet()) {
            byte[] filterDecode = byteFuseFilter.decode(key);
            Assert.assertArrayEquals(keyValueMap.get(key), filterDecode);
            int[] positions = byteFusePosition.positions(key);
            byte[] positionDecode = new byte[valueByteLength];
            for (int position : positions) {
                ByteFuseUtils.addi(positionDecode, storage[position], valueByteLength);
            }
            Assert.assertArrayEquals(keyValueMap.get(key), positionDecode);
        }
    }

    private Map<ByteBuffer, byte[]> randomKeyValueMap(int size, int valueByteLength) {
        Map<ByteBuffer, byte[]> keyValueMap = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            byte[] key = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
            byte[] value = BytesUtils.randomByteArray(valueByteLength, secureRandom);
            keyValueMap.put(ByteBuffer.wrap(key), value);
        }
        return keyValueMap;
    }
}

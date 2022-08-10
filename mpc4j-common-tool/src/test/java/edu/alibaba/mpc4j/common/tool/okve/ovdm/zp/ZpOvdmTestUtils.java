package edu.alibaba.mpc4j.common.tool.okve.ovdm.zp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Zp-OVDM测试工具类。
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
class ZpOvdmTestUtils {
    /**
     * 私有构造函数
     */
    private ZpOvdmTestUtils() {
        // empty
    }

    /**
     * 默认质数
     */
    static final BigInteger DEFAULT_PRIME = ZpManager.getPrime(CommonConstants.BLOCK_BIT_LENGTH * 2);
    /**
     * 随机状态
     */
    static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static Map<ByteBuffer, BigInteger> randomKeyValueMap(int size) {
        Map<ByteBuffer, BigInteger> keyValueMap = new HashMap<>();
        IntStream.range(0, size).forEach(index -> {
            byte[] keyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(keyBytes);
            BigInteger value = BigIntegerUtils.randomPositive(DEFAULT_PRIME, SECURE_RANDOM);
            keyValueMap.put(ByteBuffer.wrap(keyBytes), value);
        });
        return keyValueMap;
    }
}

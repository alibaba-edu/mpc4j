package edu.alibaba.mpc4j.crypto.matrix.okve.ovdm.zp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpManager;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Zp-OVDM test utilities.
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
class ZpOvdmTestUtils {
    /**
     * private constructor.
     */
    private ZpOvdmTestUtils() {
        // empty
    }

    /**
     * default prime
     */
    static final BigInteger DEFAULT_PRIME = ZpManager.getPrime(CommonConstants.BLOCK_BIT_LENGTH * 2);
    /**
     * default Zp
     */
    static final Zp DEFAULT_ZP = ZpFactory.createInstance(EnvType.STANDARD, DEFAULT_PRIME);
    /**
     * the random state
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

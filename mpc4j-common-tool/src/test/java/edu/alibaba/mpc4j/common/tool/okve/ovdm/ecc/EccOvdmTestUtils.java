package edu.alibaba.mpc4j.common.tool.okve.ovdm.ecc;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import org.bouncycastle.math.ec.ECPoint;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * ECC-OVDM测试工具类。
 *
 * @author Weiran Liu
 * @date 2022/4/19
 */
class EccOvdmTestUtils {
    /**
     * 私有构造函数
     */
    private EccOvdmTestUtils() {
        // empty
    }

    /**
     * 椭圆曲线
     */
    static final Ecc ECC = EccFactory.createInstance(EnvType.STANDARD);
    /**
     * 随机状态
     */
    static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static Map<ByteBuffer, ECPoint> randomKeyValueMap(int size) {
        Map<ByteBuffer, ECPoint> keyValueMap = new HashMap<>();
        IntStream.range(0, size).forEach(index -> {
            byte[] keyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(keyBytes);
            ECPoint value = ECC.randomPoint(SECURE_RANDOM);
            keyValueMap.put(ByteBuffer.wrap(keyBytes), value);
        });
        return keyValueMap;
    }
}

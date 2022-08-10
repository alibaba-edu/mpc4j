package edu.alibaba.mpc4j.common.tool.crypto.kdf;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory.KdfType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 密钥派生函数测试。
 *
 * @author Weiran Liu
 * @date 2021/12/31
 */
@RunWith(Parameterized.class)
public class KdfTest {
    /**
     * 最大随机轮数
     */
    private static final int MAX_RANDOM_ROUND = 400;
    /**
     * 并发数量
     */
    private static final int MAX_PARALLEL = 1 << 10;
    /**
     * 全0消息
     */
    private static final byte[] ZERO_SEED = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // NATIVE_BLAKE_3
        configurationParams.add(new Object[] {KdfType.NATIVE_BLAKE_3.name(), KdfType.NATIVE_BLAKE_3, });
        // BC_BLAKE_2B
        configurationParams.add(new Object[] {KdfType.BC_BLAKE_2B.name(), KdfType.BC_BLAKE_2B, });
        // NATIVE_BLAKE_2B
        configurationParams.add(new Object[] {KdfType.NATIVE_BLAKE_2B.name(), KdfType.NATIVE_BLAKE_2B, });
        // JDK_SHA256
        configurationParams.add(new Object[] {KdfType.JDK_SHA256.name(), KdfType.JDK_SHA256, });
        // NATIVE_SHA256
        configurationParams.add(new Object[] {KdfType.NATIVE_SHA256.name(), KdfType.NATIVE_SHA256, });
        // BC_SM3
        configurationParams.add(new Object[] {KdfType.BC_SM3.name(), KdfType.BC_SM3, });



        return configurationParams;
    }

    /**
     * 待测试的伪随机函数实例
     */
    private final KdfType kdfType;

    public KdfTest(String name, KdfType kdfType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.kdfType = kdfType;
    }

    @Test
    public void testIllegalInputs() {
        Kdf kdf = KdfFactory.createInstance(kdfType);
        try {
            // 尝试输入字节长度为0的种子
            kdf.deriveKey(new byte[0]);
            throw new IllegalStateException("ERROR: successfully call KDF with a 0-length seed");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testType() {
        Kdf kdf = KdfFactory.createInstance(kdfType);
        Assert.assertEquals(kdfType, kdf.getKdfType());
    }

    @Test
    public void testConstantInput() {
        testConstantInput(new byte[1]);
        testConstantInput(new byte[CommonConstants.STATS_BYTE_LENGTH - 1]);
        testConstantInput(new byte[CommonConstants.STATS_BYTE_LENGTH]);
        testConstantInput(new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1]);
        testConstantInput(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        testConstantInput(new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1]);
        testConstantInput(new byte[2 * CommonConstants.BLOCK_BYTE_LENGTH - 1]);
        testConstantInput(new byte[2 * CommonConstants.BLOCK_BYTE_LENGTH]);
        testConstantInput(new byte[2 * CommonConstants.BLOCK_BYTE_LENGTH + 1]);
    }

    private void testConstantInput(byte[] seed) {
        Kdf kdf = KdfFactory.createInstance(kdfType);
        // 第1次调用，输出结果不应该为全0
        byte[] key = kdf.deriveKey(seed);
        Assert.assertEquals(CommonConstants.BLOCK_BYTE_LENGTH, key.length);
        // 第2次调用，输出结果与第一次结果相同
        byte[] anKey = kdf.deriveKey(seed);
        Assert.assertArrayEquals(key, anKey);
    }

    @Test
    public void testRandomInput() {
        testRandomInput(CommonConstants.BLOCK_BYTE_LENGTH);
        testRandomInput(2 * CommonConstants.BLOCK_BYTE_LENGTH);
        testRandomInput(4 * CommonConstants.BLOCK_BYTE_LENGTH);
        testRandomInput(8 * CommonConstants.BLOCK_BYTE_LENGTH);
        testRandomInput(16 * CommonConstants.BLOCK_BYTE_LENGTH);
    }

    private void testRandomInput(int seedByteLength) {
        Set<ByteBuffer> keySet = new HashSet<>();
        Kdf kdf = KdfFactory.createInstance(kdfType);
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            byte[] randomSeed = new byte[seedByteLength];
            SECURE_RANDOM.nextBytes(randomSeed);
            keySet.add(ByteBuffer.wrap(kdf.deriveKey(randomSeed)));
        }
        Assert.assertEquals(MAX_RANDOM_ROUND, keySet.size());
    }

    @Test
    public void testParallel() {
        Kdf kdf = KdfFactory.createInstance(kdfType);
        Set<ByteBuffer> keySed = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> kdf.deriveKey(ZERO_SEED))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, keySed.size());
    }
}

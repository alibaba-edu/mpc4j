package edu.alibaba.mpc4j.common.tool.crypto.prp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;
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
 * 伪随机置换（PRP）测试类。
 *
 * @author Weiran Liu
 * @date 2021/11/30
 */
@RunWith(Parameterized.class)
public class PrpTest {
    /**
     * 最大随机轮数
     */
    private static final int MAX_RANDOM_ROUND = 400;
    /**
     * 并发数量
     */
    private static final int MAX_PARALLEL = 1 << 10;
    /**
     * 全0密钥
     */
    private static final byte[] ZERO_KEY = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * 全0明文
     */
    private static final byte[] ZERO_PLAINTEXT = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * 全0密文
     */
    private static final byte[] ZERO_CIPHERTEXT = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // NATIVE_AES
        configurationParams.add(new Object[] {PrpType.NATIVE_AES.name(), PrpType.NATIVE_AES,});
        // JDK_AES
        configurationParams.add(new Object[] {PrpType.JDK_AES.name(), PrpType.JDK_AES,});

        // BC_SM4
        configurationParams.add(new Object[] {PrpType.BC_SM4.name(), PrpType.BC_SM4,});

        // JDK_BYTES_LOW_MC_20
        configurationParams.add(new Object[] {PrpType.JDK_BYTES_LOW_MC_20.name(), PrpType.JDK_BYTES_LOW_MC_20,});
        // JDK_LONGS_LOW_MC_20
        configurationParams.add(new Object[] {PrpType.JDK_LONGS_LOW_MC_20.name(), PrpType.JDK_LONGS_LOW_MC_20,});

        // JDK_BYTES_LOW_MC_21
        configurationParams.add(new Object[] {PrpType.JDK_BYTES_LOW_MC_21.name(), PrpType.JDK_BYTES_LOW_MC_21,});
        // JDK_LONGS_LOW_MC_21
        configurationParams.add(new Object[] {PrpType.JDK_LONGS_LOW_MC_21.name(), PrpType.JDK_LONGS_LOW_MC_21,});

        // JDK_BYTES_LOW_MC_23
        configurationParams.add(new Object[] {PrpType.JDK_BYTES_LOW_MC_23.name(), PrpType.JDK_BYTES_LOW_MC_23,});
        // JDK_LONGS_LOW_MC_23
        configurationParams.add(new Object[] {PrpType.JDK_LONGS_LOW_MC_23.name(), PrpType.JDK_LONGS_LOW_MC_23,});

        // JDK_BYTES_LOW_MC_32
        configurationParams.add(new Object[] {PrpType.JDK_BYTES_LOW_MC_32.name(), PrpType.JDK_BYTES_LOW_MC_32,});
        // JDK_LONGS_LOW_MC_32
        configurationParams.add(new Object[] {PrpType.JDK_LONGS_LOW_MC_32.name(), PrpType.JDK_LONGS_LOW_MC_32,});

        // JDK_BYTES_LOW_MC_192
        configurationParams.add(new Object[] {PrpType.JDK_BYTES_LOW_MC_192.name(), PrpType.JDK_BYTES_LOW_MC_192,});
        // JDK_LONGS_LOW_MC_192
        configurationParams.add(new Object[] {PrpType.JDK_LONGS_LOW_MC_192.name(), PrpType.JDK_LONGS_LOW_MC_192,});

        // JDK_BYTES_LOW_MC_208
        configurationParams.add(new Object[] {PrpType.JDK_BYTES_LOW_MC_208.name(), PrpType.JDK_BYTES_LOW_MC_208,});
        // JDK_LONGS_LOW_MC_208
        configurationParams.add(new Object[] {PrpType.JDK_LONGS_LOW_MC_208.name(), PrpType.JDK_LONGS_LOW_MC_208,});

        // JDK_BYTES_LOW_MC_287
        configurationParams.add(new Object[] {PrpType.JDK_BYTES_LOW_MC_287.name(), PrpType.JDK_BYTES_LOW_MC_287,});
        // JDK_LONGS_LOW_MC_287
        configurationParams.add(new Object[] {PrpType.JDK_LONGS_LOW_MC_287.name(), PrpType.JDK_LONGS_LOW_MC_287,});

        return configurationParams;
    }

    /**
     * 待测试的PRP类型
     */
    public final PrpType type;

    public PrpTest(String name, PrpType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testIllegalInputs() {
        Prp prp = PrpFactory.createInstance(type);
        try {
            // 尝试设置短密钥
            prp.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1]);
            throw new IllegalStateException("ERROR: successfully set key with length less than 16 bytes");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试设置长密钥
            prp.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1]);
            throw new IllegalStateException("ERROR: successfully set key with length larger than 16 bytes");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试在未设置好密钥的时候执行PRP
            prp.prp(ZERO_PLAINTEXT);
            throw new IllegalStateException("ERROR: successfully call prp without setting key");
        } catch (AssertionError ignored) {

        }
        prp.setKey(ZERO_KEY);
        try {
            // 尝试短明文输入
            prp.prp(new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1]);
            throw new IllegalStateException("ERROR: successfully prp plaintext with length less than 16 bytes");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试短密文输入
            prp.invPrp(new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1]);
            throw new IllegalStateException("ERROR: successfully inv prp plaintext with length less than 16 bytes");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试长明文输入
            prp.prp(new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1]);
            throw new IllegalStateException("ERROR: successfully prp plaintext with length larger than 16 bytes");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试长密文输入
            prp.invPrp(new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1]);
            throw new IllegalStateException("ERROR: successfully inv prp plaintext with length larger than 16 bytes");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testType() {
        Prp prp = PrpFactory.createInstance(type);
        Assert.assertEquals(type, prp.getPrpType());
    }

    @Test
    public void testConstantPrp() {
        Prp prp = PrpFactory.createInstance(type);
        prp.setKey(ZERO_KEY);
        // 先Prp再InvPrp，结果相同
        byte[] ciphertext = prp.prp(ZERO_PLAINTEXT);
        byte[] decryptPlaintext = prp.invPrp(ciphertext);
        Assert.assertEquals(ByteBuffer.wrap(ZERO_PLAINTEXT), ByteBuffer.wrap(decryptPlaintext));
        // 两次调用Prp，结果相同
        byte[] anCiphertext = prp.prp(ZERO_PLAINTEXT);
        Assert.assertEquals(ByteBuffer.wrap(ciphertext), ByteBuffer.wrap(anCiphertext));
        // 先InvPrp再Prp，结果相同
        byte[] plaintext = prp.invPrp(ZERO_CIPHERTEXT);
        byte[] encryptCiphertext = prp.prp(plaintext);
        Assert.assertEquals(ByteBuffer.wrap(ZERO_CIPHERTEXT), ByteBuffer.wrap(encryptCiphertext));
        // 两次调用Prp，结果相同
        byte[] anPlaintext = prp.invPrp(ZERO_CIPHERTEXT);
        Assert.assertEquals(ByteBuffer.wrap(plaintext), ByteBuffer.wrap(anPlaintext));
    }

    @Test
    public void testRandomKeyPrp() {
        Set<ByteBuffer> randomKeyPrpSet = new HashSet<>();
        Set<ByteBuffer> randomKeyInvPrpSet = new HashSet<>();
        // 不同密钥，相同明文/相同密文的结果应不相同
        Prp prp = PrpFactory.createInstance(type);
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            byte[] randomKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(randomKey);
            prp.setKey(randomKey);
            randomKeyPrpSet.add(ByteBuffer.wrap(prp.prp(ZERO_PLAINTEXT)));
            randomKeyInvPrpSet.add(ByteBuffer.wrap(prp.invPrp(ZERO_CIPHERTEXT)));
        }
        Assert.assertEquals(MAX_RANDOM_ROUND, randomKeyPrpSet.size());
        Assert.assertEquals(MAX_RANDOM_ROUND, randomKeyInvPrpSet.size());
    }

    @Test
    public void testModifyKey() {
        Prp prp = PrpFactory.createInstance(type);
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        prp.setKey(key);
        byte[] ciphertext = prp.prp(ZERO_PLAINTEXT);
        byte[] plaintext = prp.invPrp(ZERO_CIPHERTEXT);
        // 外部故意修改密钥，应不影响处理结果
        SECURE_RANDOM.nextBytes(key);
        byte[] anCiphertext = prp.prp(ZERO_PLAINTEXT);
        byte[] anPlaintext = prp.invPrp(ZERO_CIPHERTEXT);
        Assert.assertArrayEquals(ciphertext, anCiphertext);
        Assert.assertArrayEquals(plaintext, anPlaintext);
    }

    @Test
    public void testRandomPlaintextPrp() {
        Set<ByteBuffer> randomPlaintextPrpSet = new HashSet<>();
        Prp prp = PrpFactory.createInstance(type);
        // 不同明文，相同密钥的密文结果应不相同
        prp.setKey(ZERO_KEY);
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            byte[] randomPlaintext = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(randomPlaintext);
            randomPlaintextPrpSet.add(ByteBuffer.wrap(prp.prp(randomPlaintext)));
        }
        Assert.assertEquals(MAX_RANDOM_ROUND, randomPlaintextPrpSet.size());
    }

    @Test
    public void testRandomCiphertextInvPrp() {
        Set<ByteBuffer> randomCiphertextInvPrpSet = new HashSet<>();
        Prp prp = PrpFactory.createInstance(type);
        // 不同密文，相同密钥的密文结果应不相同
        prp.setKey(ZERO_KEY);
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            byte[] randomCiphertext = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(randomCiphertext);
            randomCiphertextInvPrpSet.add(ByteBuffer.wrap(prp.invPrp(randomCiphertext)));
        }
        Assert.assertEquals(MAX_RANDOM_ROUND, randomCiphertextInvPrpSet.size());
    }

    @Test
    public void testParallelPrp() {
        Prp prp = PrpFactory.createInstance(type);
        prp.setKey(ZERO_KEY);
        Set<ByteBuffer> ciphertextSet = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> prp.prp(ZERO_PLAINTEXT))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, ciphertextSet.size());
    }

    @Test
    public void testParallelInvPrp() {
        Prp prp = PrpFactory.createInstance(type);
        prp.setKey(ZERO_KEY);
        Set<ByteBuffer> plaintextSet = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> prp.invPrp(ZERO_CIPHERTEXT))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, plaintextSet.size());
    }
}
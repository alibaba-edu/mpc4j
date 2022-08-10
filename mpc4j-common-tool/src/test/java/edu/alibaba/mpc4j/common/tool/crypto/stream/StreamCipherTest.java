package edu.alibaba.mpc4j.common.tool.crypto.stream;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipherFactory.StreamCipherType;
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
 * 流密码测试。
 *
 * @author Weiran Liu
 * @date 2022/8/9
 */
@RunWith(Parameterized.class)
public class StreamCipherTest {
    /**
     * 最大随机轮数
     */
    private static final int MAX_RANDOM_ROUND = 400;
    /**
     * 并发数量
     */
    private static final int MAX_PARALLEL = 1 << 10;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // JDK_AES_OFB
        configurationParams.add(new Object[] {StreamCipherType.JDK_AES_OFB.name(), StreamCipherType.JDK_AES_OFB,});
        // BC_AES_OFB
        configurationParams.add(new Object[] {StreamCipherType.BC_AES_OFB.name(), StreamCipherType.BC_AES_OFB,});
        // BC_SM4_OFB
        configurationParams.add(new Object[] {StreamCipherType.BC_SM4_OFB.name(), StreamCipherType.BC_SM4_OFB,});
        // BC_ZUC_128
        configurationParams.add(new Object[] {StreamCipherType.BC_ZUC_128.name(), StreamCipherType.BC_ZUC_128,});

        return configurationParams;
    }

    /**
     * 待测试的流密码类型
     */
    private final StreamCipherType type;
    /**
     * 流密码
     */
    private final StreamCipher streamCipher;
    /**
     * IV字节长度
     */
    private final int ivByteLength;
    /**
     * 默认密钥
     */
    private final byte[] defaultKey;
    /**
     * 默认初始向量
     */
    private final byte[] defaultIv;
    /**
     * 默认明文
     */
    private final byte[] defaultPlaintext;

    public StreamCipherTest(String name, StreamCipherType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
        streamCipher = StreamCipherFactory.createInstance(type);
        ivByteLength = streamCipher.ivByteLength();
        defaultIv = new byte[ivByteLength];
        defaultKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        defaultPlaintext = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    }

    @Test
    public void testIllegalInputs() {
        try {
            // 短密钥加密
            streamCipher.encrypt(new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1], defaultIv, defaultPlaintext);
            throw new IllegalStateException("ERROR: successfully set key with length less than 16 bytes");
        } catch (AssertionError ignored) {

        }
        try {
            // 长密钥加密
            streamCipher.encrypt(new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1], defaultIv, defaultPlaintext);
            throw new IllegalStateException("ERROR: successfully set key with length larger than 16 bytes");
        } catch (AssertionError ignored) {

        }
        try {
            // 短IV加密
            streamCipher.encrypt(defaultKey, new byte[ivByteLength - 1], defaultPlaintext);
            throw new IllegalStateException("ERROR: successfully set key with IV less than " + ivByteLength + " bytes");
        } catch (AssertionError ignored) {

        }
        try {
            // 长密钥加密
            streamCipher.encrypt(new byte[ivByteLength + 1], defaultIv, defaultPlaintext);
            throw new IllegalStateException("ERROR: successfully set key with IV larger than " + ivByteLength + " bytes");
        } catch (AssertionError ignored) {

        }
        byte[] ciphertext = new byte[ivByteLength];
        try {
            // 尝试短密文解密
            streamCipher.ivDecrypt(defaultKey, ciphertext);
            throw new IllegalStateException("ERROR: successfully decrypt with ciphertext length no more than 16 bytes");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testType() {
        StreamCipher streamCipher = StreamCipherFactory.createInstance(type);
        Assert.assertEquals(type, streamCipher.getType());
    }

    @Test
    public void testBlockEncryption() {
        // 不带IV，先加密再解密，结果相同
        byte[] ciphertext = streamCipher.encrypt(defaultKey, defaultIv, defaultPlaintext);
        Assert.assertEquals(defaultPlaintext.length, ciphertext.length);
        byte[] decryptPlaintext = streamCipher.decrypt(defaultKey, defaultIv, ciphertext);
        Assert.assertEquals(ByteBuffer.wrap(defaultPlaintext), ByteBuffer.wrap(decryptPlaintext));
        // 相同密钥，相同IV，加密结果相同
        byte[] anCiphertext = streamCipher.encrypt(defaultKey, defaultIv, defaultPlaintext);
        Assert.assertEquals(ByteBuffer.wrap(ciphertext), ByteBuffer.wrap(anCiphertext));
        // 带IV，先加密再解密，结果相同
        ciphertext = streamCipher.ivEncrypt(defaultKey, defaultIv, defaultPlaintext);
        Assert.assertEquals(ivByteLength + defaultPlaintext.length, ciphertext.length);
        decryptPlaintext = streamCipher.ivDecrypt(defaultKey, ciphertext);
        Assert.assertEquals(ByteBuffer.wrap(defaultPlaintext), ByteBuffer.wrap(decryptPlaintext));
        // 相同密钥，相同IV，加密结果相同
        anCiphertext = streamCipher.ivEncrypt(defaultKey, defaultIv, defaultPlaintext);
        Assert.assertEquals(ByteBuffer.wrap(ciphertext), ByteBuffer.wrap(anCiphertext));
    }

    @Test
    public void testEncryption() {
        for (int byteLength = 1; byteLength < CommonConstants.BLOCK_BYTE_LENGTH * 8; byteLength++) {
            byte[] plaintext = new byte[byteLength];
            // 不带IV，先加密再解密，结果相同
            byte[] ciphertext = streamCipher.encrypt(defaultKey, defaultIv, plaintext);
            Assert.assertEquals(plaintext.length, ciphertext.length);
            byte[] decryptPlaintext = streamCipher.decrypt(defaultKey, defaultIv, ciphertext);
            Assert.assertEquals(ByteBuffer.wrap(plaintext), ByteBuffer.wrap(decryptPlaintext));
            // 相同密钥，相同IV，加密结果相同
            byte[] anCiphertext = streamCipher.encrypt(defaultKey, defaultIv, plaintext);
            Assert.assertEquals(ByteBuffer.wrap(ciphertext), ByteBuffer.wrap(anCiphertext));
            // 带IV，先加密再解密，结果相同
            ciphertext = streamCipher.ivEncrypt(defaultKey, defaultIv, plaintext);
            Assert.assertEquals(ivByteLength + plaintext.length, ciphertext.length);
            decryptPlaintext = streamCipher.ivDecrypt(defaultKey, ciphertext);
            Assert.assertEquals(ByteBuffer.wrap(plaintext), ByteBuffer.wrap(decryptPlaintext));
            // 相同密钥，相同IV，加密结果相同
            anCiphertext = streamCipher.ivEncrypt(defaultKey, defaultIv, plaintext);
            Assert.assertEquals(ByteBuffer.wrap(ciphertext), ByteBuffer.wrap(anCiphertext));
        }
    }

    @Test
    public void testRandomKeyStreamCipher() {
        Set<ByteBuffer> ciphertextSet = new HashSet<>(MAX_RANDOM_ROUND);
        Set<ByteBuffer> ivCiphertextSet = new HashSet<>(MAX_RANDOM_ROUND);
        // 不同密钥，相同密文的结果应不相同
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            byte[] randomKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(randomKey);
            ciphertextSet.add(ByteBuffer.wrap(streamCipher.encrypt(randomKey, defaultIv, defaultPlaintext)));
            ivCiphertextSet.add(ByteBuffer.wrap(streamCipher.ivEncrypt(randomKey, defaultIv, defaultPlaintext)));
        }
        Assert.assertEquals(MAX_RANDOM_ROUND, ciphertextSet.size());
        Assert.assertEquals(MAX_RANDOM_ROUND, ivCiphertextSet.size());
    }

    @Test
    public void testRandomIvStreamCipher() {
        Set<ByteBuffer> ciphertextSet = new HashSet<>(MAX_RANDOM_ROUND);
        Set<ByteBuffer> ivCiphertextSet = new HashSet<>(MAX_RANDOM_ROUND);
        // 不同IV，相同密文的结果应不相同
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            byte[] randomIv = new byte[ivByteLength];
            SECURE_RANDOM.nextBytes(randomIv);
            ciphertextSet.add(ByteBuffer.wrap(streamCipher.encrypt(defaultKey, randomIv, defaultPlaintext)));
            ivCiphertextSet.add(ByteBuffer.wrap(streamCipher.ivEncrypt(defaultKey, randomIv, defaultPlaintext)));
        }
        Assert.assertEquals(MAX_RANDOM_ROUND, ciphertextSet.size());
        Assert.assertEquals(MAX_RANDOM_ROUND, ivCiphertextSet.size());
    }

    @Test
    public void testParallel() {
        // encrypt并发
        Set<ByteBuffer> ciphertextSet = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> streamCipher.encrypt(defaultKey, defaultIv, defaultPlaintext))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, ciphertextSet.size());
        byte[] ciphertext = streamCipher.encrypt(defaultKey, defaultIv, defaultPlaintext);
        // ivEncrypt并发
        Set<ByteBuffer> ivCiphertextSet = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> streamCipher.ivEncrypt(defaultKey, defaultIv, defaultPlaintext))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, ivCiphertextSet.size());
        byte[] ivCiphertext = streamCipher.ivEncrypt(defaultKey, defaultIv, defaultPlaintext);
        // decrypt并发
        Set<ByteBuffer> plaintextSet = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> streamCipher.decrypt(defaultKey, defaultIv, ciphertext))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, plaintextSet.size());
        // ivDecrypt并发
        Set<ByteBuffer> ivPlaintextSet = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> streamCipher.ivDecrypt(defaultKey, ivCiphertext))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, ivPlaintextSet.size());
    }
}

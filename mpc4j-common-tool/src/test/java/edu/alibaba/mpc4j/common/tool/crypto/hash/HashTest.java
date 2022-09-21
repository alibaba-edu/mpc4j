package edu.alibaba.mpc4j.common.tool.crypto.hash;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory.HashType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
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
 * 哈希函数测试类。
 *
 * @author Weiran Liu
 * @date 2021/12/05
 */
@RunWith(Parameterized.class)
public class HashTest {
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
    private static final byte[] ZERO_MESSAGE = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // NATIVE_BLAKE_2B_160
        configurationParams.add(new Object[]{HashType.NATIVE_BLAKE_3.name(), HashType.NATIVE_BLAKE_3,});
        // NATIVE_BLAKE_2B_160
        configurationParams.add(new Object[]{HashType.NATIVE_BLAKE_2B_160.name(), HashType.NATIVE_BLAKE_2B_160,});
        // BC_BLAKE_2B_160
        configurationParams.add(new Object[]{HashType.BC_BLAKE_2B_160.name(), HashType.BC_BLAKE_2B_160,});
        // NATIVE_SHA256
        configurationParams.add(new Object[]{HashType.NATIVE_SHA256.name(), HashType.NATIVE_SHA256,});
        // JDK_SHA256
        configurationParams.add(new Object[]{HashType.JDK_SHA256.name(), HashType.JDK_SHA256,});
        // BC_SHAKE_128
        configurationParams.add(new Object[]{HashType.BC_SHAKE_128.name(), HashType.BC_SHAKE_128,});
        // BC_SHAKE_256
        configurationParams.add(new Object[]{HashType.BC_SHAKE_256.name(), HashType.BC_SHAKE_256,});
        // BC_SHA3_256
        configurationParams.add(new Object[]{HashType.BC_SHA3_256.name(), HashType.BC_SHA3_256,});
        // BC_SHA3_512
        configurationParams.add(new Object[]{HashType.BC_SHA3_512.name(), HashType.BC_SHA3_512,});
        // BC_SM3
        configurationParams.add(new Object[]{HashType.BC_SM3.name(), HashType.BC_SM3,});

        return configurationParams;
    }

    /**
     * 待测试的哈希函数类型
     */
    private final HashType hashType;

    public HashTest(String name, HashType hashType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.hashType = hashType;
    }

    @Test
    public void testIllegalInputs() {
        try {
            // 尝试设置输出为0字节的哈希函数
            HashFactory.createInstance(hashType, 0);
            throw new IllegalStateException("ERROR: successfully create hash with 0 output byte length");
        } catch (AssertionError ignored) {

        }
        Hash hash = HashFactory.createInstance(hashType, CommonConstants.BLOCK_BYTE_LENGTH);
        try {
            // 尝试输入字节长度为0的消息
            hash.digestToBytes(new byte[0]);
            throw new IllegalStateException("ERROR: successfully hash a message with 0 byte length");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testType() {
        Hash hash = HashFactory.createInstance(hashType, CommonConstants.BLOCK_BYTE_LENGTH);
        Assert.assertEquals(hashType, hash.getHashType());
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

    private void testConstantInput(byte[] input) {
        Hash hash = HashFactory.createInstance(hashType, CommonConstants.BLOCK_BYTE_LENGTH);
        // 第1次调用，输出结果不应该为全0
        byte[] output = hash.digestToBytes(input);
        Assert.assertFalse(BytesUtils.equals(new byte[hash.getOutputByteLength()], output));
        // 第2次调用，输出结果与第一次结果相同
        byte[] anOutput = hash.digestToBytes(input);
        Assert.assertArrayEquals(output, anOutput);
    }

    @Test
    public void testRandomInput() {
        testRandomInput(CommonConstants.STATS_BYTE_LENGTH);
        testRandomInput(CommonConstants.BLOCK_BYTE_LENGTH - 1);
        testRandomInput(CommonConstants.BLOCK_BYTE_LENGTH);
        testRandomInput(CommonConstants.BLOCK_BYTE_LENGTH + 1);
        testRandomInput(2 * CommonConstants.BLOCK_BYTE_LENGTH - 1);
        testRandomInput(2 * CommonConstants.BLOCK_BYTE_LENGTH);
        testRandomInput(2 * CommonConstants.BLOCK_BYTE_LENGTH + 1);
    }

    private void testRandomInput(int inputByteLength) {
        Set<ByteBuffer> outputSet = new HashSet<>();
        Hash hash = HashFactory.createInstance(hashType, CommonConstants.BLOCK_BYTE_LENGTH);
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            byte[] randomMessage = new byte[inputByteLength];
            SECURE_RANDOM.nextBytes(randomMessage);
            outputSet.add(ByteBuffer.wrap(hash.digestToBytes(randomMessage)));
        }
        Assert.assertEquals(MAX_RANDOM_ROUND, outputSet.size());
    }

    @Test
    public void testConstantOutput() {
        testConstantOutput(1);
        testConstantOutput(CommonConstants.STATS_BYTE_LENGTH - 1);
        testConstantOutput(CommonConstants.STATS_BYTE_LENGTH);
        testConstantOutput(CommonConstants.STATS_BYTE_LENGTH + 1);
        testConstantOutput(CommonConstants.BLOCK_BYTE_LENGTH - 1);
        testConstantOutput(CommonConstants.BLOCK_BYTE_LENGTH);
        testConstantOutput(CommonConstants.BLOCK_BYTE_LENGTH + 1);
        testConstantOutput(2 * CommonConstants.BLOCK_BYTE_LENGTH - 1);
        testConstantOutput(2 * CommonConstants.BLOCK_BYTE_LENGTH);
        testConstantOutput(2 * CommonConstants.BLOCK_BYTE_LENGTH + 1);
    }

    private void testConstantOutput(int outputByteLength) {
        if (outputByteLength <= HashFactory.getUnitByteLength(hashType)) {
            Hash hash = HashFactory.createInstance(hashType, outputByteLength);
            // 第1次调用，输出长度应为指定的输出长度
            byte[] output = hash.digestToBytes(ZERO_MESSAGE);
            Assert.assertEquals(outputByteLength, output.length);
            // 第2次调用，输出结果与第一次结果相同
            byte[] anOutput = hash.digestToBytes(ZERO_MESSAGE);
            Assert.assertArrayEquals(output, anOutput);
        }
    }

    @Test
    public void testRandomOutput() {
        testRandomOutput(CommonConstants.STATS_BYTE_LENGTH);
        testRandomOutput(CommonConstants.BLOCK_BYTE_LENGTH - 1);
        testRandomOutput(CommonConstants.BLOCK_BYTE_LENGTH);
        testRandomOutput(CommonConstants.BLOCK_BYTE_LENGTH + 1);
        testRandomOutput(2 * CommonConstants.BLOCK_BYTE_LENGTH - 1);
        testRandomOutput(2 * CommonConstants.BLOCK_BYTE_LENGTH);
        testRandomOutput(2 * CommonConstants.BLOCK_BYTE_LENGTH + 1);
    }

    private void testRandomOutput(int outputByteLength) {
        if (outputByteLength <= HashFactory.getUnitByteLength(hashType)) {
            Set<ByteBuffer> outputSet = new HashSet<>();
            Hash hash = HashFactory.createInstance(hashType, outputByteLength);
            for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
                byte[] randomMessage = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                SECURE_RANDOM.nextBytes(randomMessage);
                outputSet.add(ByteBuffer.wrap(hash.digestToBytes(randomMessage)));
            }
            Assert.assertEquals(MAX_RANDOM_ROUND, outputSet.size());
        }
    }

    @Test
    public void testParallel() {
        Hash hash = HashFactory.createInstance(hashType, CommonConstants.BLOCK_BYTE_LENGTH);
        Set<ByteBuffer> hashSet = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> hash.digestToBytes(ZERO_MESSAGE))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, hashSet.size());
    }
}

package edu.alibaba.mpc4j.common.tool.f3hash;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.f3hash.F3HashFactory.F3HashType;
import edu.alibaba.mpc4j.common.tool.hash.LongHashFactory.LongHashType;
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
 * F3Hash test
 *
 * @author Feng Han
 * @date 2024/10/21
 */
@RunWith(Parameterized.class)
public class F3HashTest {
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

        // XXHash
        configurationParams.add(new Object[]{
            F3HashType.LONG_F3_HASH.name() + "_" + LongHashType.BOB_HASH_64.name(),
            new LongF3Hash(LongHashType.BOB_HASH_64)
        });
        // BobHash
        configurationParams.add(new Object[]{
            F3HashType.LONG_F3_HASH.name() + "_" + LongHashType.XX_HASH_64.name(),
            new LongF3Hash(LongHashType.XX_HASH_64)});

        return configurationParams;
    }

    /**
     * 待测试的哈希函数类型
     */
    private final F3Hash hash;

    public F3HashTest(String name, F3Hash f3hash) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.hash = f3hash;
    }

    @Test
    public void testIllegalInputs() {
        try {
            // 尝试输入字节长度为0的消息
            hash.digestToBytes(new byte[0]);
            throw new IllegalStateException("ERROR: successfully hash a message with 0 byte length");
        } catch (AssertionError ignored) {

        }
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
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            byte[] randomMessage = new byte[inputByteLength];
            SECURE_RANDOM.nextBytes(randomMessage);
            outputSet.add(ByteBuffer.wrap(hash.digestToBytes(randomMessage)));
        }
        Assert.assertEquals(MAX_RANDOM_ROUND, outputSet.size());
    }

    @Test
    public void testParallel() {
        Set<ByteBuffer> hashSet = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> hash.digestToBytes(ZERO_MESSAGE))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, hashSet.size());
    }
}

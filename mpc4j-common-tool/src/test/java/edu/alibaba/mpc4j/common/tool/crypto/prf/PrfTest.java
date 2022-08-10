package edu.alibaba.mpc4j.common.tool.crypto.prf;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory.PrfType;
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
 * 伪随机函数测试。
 *
 * @author Weiran Liu
 * @date 2021/12/12
 */
@RunWith(Parameterized.class)
public class PrfTest {
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
     * 全0密钥
     */
    private static final byte[] ZERO_KEY = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // BC_SIP_HASH
        configurationParams.add(new Object[] {PrfType.BC_SIP_HASH.name(), PrfType.BC_SIP_HASH, });
        // BC_SIP128_HASH
        configurationParams.add(new Object[] {PrfType.BC_SIP128_HASH.name(), PrfType.BC_SIP128_HASH, });
        // JDK_AES_CBC
        configurationParams.add(new Object[] {PrfType.JDK_AES_CBC.name(), PrfType.JDK_AES_CBC, });
        // BC_SM4_CBC
        configurationParams.add(new Object[] {PrfType.BC_SM4_CBC.name(), PrfType.BC_SM4_CBC, });

        return configurationParams;
    }

    /**
     * 待测试的伪随机函数实例
     */
    private final PrfType prfType;

    public PrfTest(String name, PrfType prfType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.prfType = prfType;
    }

    @Test
    public void testIllegalInputs() {
        try {
            // 尝试设置输出为0字节的伪随机函数
            PrfFactory.createInstance(prfType, 0);
            throw new IllegalStateException("ERROR: successfully create PRF with 0 output byte length");
        } catch (AssertionError ignored) {

        }
        Prf prf = PrfFactory.createInstance(prfType, Integer.BYTES - 1);
        try {
            // 尝试设置短密钥
            prf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1]);
            throw new IllegalStateException("ERROR: successfully set key with length less than 16 bytes");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试设置长密钥
            prf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1]);
            throw new IllegalStateException("ERROR: successfully set key with length larger than 16 bytes");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试在未设置好密钥的时候执行PRP
            prf.getBytes(ZERO_MESSAGE);
            throw new IllegalStateException("ERROR: successfully call PRF without setting key");
        } catch (AssertionError ignored) {

        }
        prf.setKey(ZERO_KEY);
        try {
            // 尝试输入字节长度为0的消息
            prf.getBytes(new byte[0]);
            throw new IllegalStateException("ERROR: successfully call PRF with a 0-length message");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试对输出字节长度小于32比特的PRF取整数
            prf.getInteger(ZERO_MESSAGE, Integer.MAX_VALUE);
            throw new IllegalStateException("ERROR: successfully call getInteger with less PRF output length");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试对输出字节长度小于32比特的PRF取长整数
            prf.getLong(ZERO_MESSAGE, Long.MAX_VALUE);
            throw new IllegalStateException("ERROR: successfully call getLong with less PRF output length");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试对输出字节长度小于32比特的PRF取浮点数
            prf.getDouble(ZERO_MESSAGE);
            throw new IllegalStateException("ERROR: successfully call getDouble with less PRF output length");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testType() {
        Prf prf = PrfFactory.createInstance(prfType, CommonConstants.BLOCK_BYTE_LENGTH);
        Assert.assertEquals(prfType, prf.getPrfType());
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
        Prf prf = PrfFactory.createInstance(prfType, CommonConstants.BLOCK_BYTE_LENGTH);
        prf.setKey(ZERO_KEY);
        // 第1次调用，输出结果不应该为全0
        byte[] output = prf.getBytes(input);
        Assert.assertFalse(BytesUtils.equals(new byte[prf.getOutputByteLength()], output));
        // 第2次调用，输出结果与第一次结果相同
        byte[] anOutput = prf.getBytes(input);
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
        Prf prf = PrfFactory.createInstance(prfType, CommonConstants.BLOCK_BYTE_LENGTH);
        prf.setKey(ZERO_KEY);
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            byte[] randomMessage = new byte[inputByteLength];
            SECURE_RANDOM.nextBytes(randomMessage);
            outputSet.add(ByteBuffer.wrap(prf.getBytes(randomMessage)));
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
        Prf prf = PrfFactory.createInstance(prfType, outputByteLength);
        prf.setKey(ZERO_KEY);
        // 第1次调用，输出长度应为指定的输出长度
        byte[] output = prf.getBytes(ZERO_MESSAGE);
        Assert.assertEquals(outputByteLength, output.length);
        // 第2次调用，输出结果与第一次结果相同
        byte[] anOutput = prf.getBytes(ZERO_MESSAGE);
        Assert.assertArrayEquals(output, anOutput);
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
        Set<ByteBuffer> outputSet = new HashSet<>();
        Prf prf = PrfFactory.createInstance(prfType, outputByteLength);
        prf.setKey(ZERO_KEY);
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            byte[] randomMessage = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(randomMessage);
            outputSet.add(ByteBuffer.wrap(prf.getBytes(randomMessage)));
        }
        Assert.assertEquals(MAX_RANDOM_ROUND, outputSet.size());
    }

    @Test
    public void testConstantKey() {
        Prf prf = PrfFactory.createInstance(prfType, CommonConstants.BLOCK_BYTE_LENGTH);
        prf.setKey(ZERO_KEY);
        // 两次调用结果相同
        byte[] output = prf.getBytes(ZERO_MESSAGE);
        Assert.assertEquals(CommonConstants.BLOCK_BYTE_LENGTH, output.length);
        byte[] anOutput = prf.getBytes(ZERO_MESSAGE);
        Assert.assertArrayEquals(output, anOutput);
    }

    @Test
    public void testRandomKey() {
        Set<ByteBuffer> randomKeyPrfSet = new HashSet<>();
        // 不同密钥，相同消息的结果应不相同
        Prf prf = PrfFactory.createInstance(prfType, CommonConstants.BLOCK_BYTE_LENGTH);
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            byte[] randomKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(randomKey);
            prf.setKey(randomKey);
            randomKeyPrfSet.add(ByteBuffer.wrap(prf.getBytes(ZERO_MESSAGE)));
        }
        Assert.assertEquals(MAX_RANDOM_ROUND, randomKeyPrfSet.size());
    }

    @Test
    public void testModifyKey() {
        Prf prf = PrfFactory.createInstance(prfType, CommonConstants.BLOCK_BYTE_LENGTH);
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        byte[] copiedKey = BytesUtils.clone(key);
        prf.setKey(key);
        byte[] output = prf.getBytes(ZERO_MESSAGE);
        // 外部故意修改密钥，应不影响处理结果
        SECURE_RANDOM.nextBytes(key);
        byte[] anOutput = prf.getBytes(ZERO_MESSAGE);
        Assert.assertArrayEquals(copiedKey, prf.getKey());
        Assert.assertArrayEquals(output, anOutput);
    }

    @Test
    public void testParallel() {
        Prf prf = PrfFactory.createInstance(prfType, CommonConstants.BLOCK_BYTE_LENGTH);
        prf.setKey(ZERO_KEY);
        Set<ByteBuffer> outputSet = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> prf.getBytes(ZERO_MESSAGE))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, outputSet.size());
    }
}

package edu.alibaba.mpc4j.common.structure.lpn.primal;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * d-本地线性编码测试。
 *
 * @author Weiran Liu
 * @date 2022/01/31
 */
@RunWith(Parameterized.class)
public class LocalLinearCoderTest {
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();

        // 论文的初始化LPN参数：k = 37248, n = 616092
        configurationParams.add(new Object[] {"k = 37248, n = 616092", 37248, 616092});
        // 论文的迭代LPN参数：k = 588160, n = 10616092
        configurationParams.add(new Object[] {"k = 588160, n = 10616092", 588160, 616092});

        return configurationParams;
    }

    /**
     * 输入数量
     */
    private final int k;
    /**
     * 输入字节数量
     */
    private final int byteK;
    /**
     * 输出数量
     */
    private final int n;
    /**
     * 输出字节数量
     */
    private final int byteN;

    public LocalLinearCoderTest(String name, int k, int n) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.k = k;
        byteK = CommonUtils.getByteLength(k);
        this.n = n;
        byteN = CommonUtils.getByteLength(n);
    }

    @Test
    public void testGf2eEncode() {
        testGf2eEncode(false);
    }

    @Test
    public void testParallelGf2eEncode() {
        testGf2eEncode(true);
    }

    private void testGf2eEncode(boolean parallel) {
        // 随机种子，初始化本地线性编码
        byte[] seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(seed);
        LocalLinearCoder localLinearCoder = new LocalLinearCoder(k, n, seed);
        localLinearCoder.setParallel(parallel);
        // 编码不同的GF2E域
        for (int inputByteLength = 1; inputByteLength <= CommonConstants.BLOCK_BYTE_LENGTH; inputByteLength = inputByteLength << 1) {
            int currentInputByteLength = inputByteLength;
            // 随机输入
            byte[][] inputs = IntStream.range(0, k)
                .mapToObj(index -> {
                    byte[] input = new byte[currentInputByteLength];
                    SECURE_RANDOM.nextBytes(input);
                    return input;
                })
                .toArray(byte[][]::new);
            Set<ByteBuffer> outputSet = Arrays.stream(localLinearCoder.encode(inputs))
                .map(ByteBuffer::wrap)
                .collect(Collectors.toSet());
            Set<ByteBuffer> anOutputSet = Arrays.stream(localLinearCoder.encode(inputs))
                .map(ByteBuffer::wrap)
                .collect(Collectors.toSet());
            // 执行两次编码，结果应该一致
            Assert.assertTrue(outputSet.containsAll(anOutputSet));
            Assert.assertTrue(anOutputSet.containsAll(outputSet));
        }
    }

    @Test
    public void testBinaryEncode() {
        testBinaryEncode(false);
    }

    @Test
    public void testParallelBinaryEncode() {
        testBinaryEncode(true);
    }

    private void testBinaryEncode(boolean parallel) {
        // 随机种子，初始化本地线性编码
        byte[] seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(seed);
        LocalLinearCoder localLinearCoder = new LocalLinearCoder(k, n, seed);
        localLinearCoder.setParallel(parallel);
        // 随机输入
        boolean[] inputs = new boolean[k];
        IntStream.range(0, k).forEach(index -> inputs[index] = SECURE_RANDOM.nextBoolean());
        boolean[] outputs = localLinearCoder.encode(inputs);
        boolean[] anOutputs = localLinearCoder.encode(inputs);
        // 执行两次编码，结果应该一致
        Assert.assertArrayEquals(outputs, anOutputs);
    }

    @Test
    public void testZ2Encode() {
        testZ2Encode(false);
    }

    @Test
    public void testParallelZ2Encode() {
        testZ2Encode(true);
    }

    private void testZ2Encode(boolean parallel) {
        // 随机种子，初始化本地线性编码
        byte[] seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(seed);
        LocalLinearCoder localLinearCoder = new LocalLinearCoder(k, n, seed);
        localLinearCoder.setParallel(parallel);
        // 随机输入
        byte[] inputs = new byte[byteK];
        SECURE_RANDOM.nextBytes(inputs);
        BytesUtils.reduceByteArray(inputs, k);
        // 第一次编码，检查长度
        byte[] outputs = localLinearCoder.encode(inputs);
        Assert.assertEquals(byteN, outputs.length);
        Assert.assertTrue(BytesUtils.isReduceByteArray(outputs, n));
        // 执行两次编码，结果应该一致
        byte[] anOutputs = localLinearCoder.encode(inputs);
        Assert.assertArrayEquals(outputs, anOutputs);
        // 更换密钥编码，检查长度
        SECURE_RANDOM.nextBytes(seed);
        localLinearCoder = new LocalLinearCoder(k, n, seed);
        localLinearCoder.setParallel(parallel);
        anOutputs = localLinearCoder.encode(inputs);
        Assert.assertEquals(byteN, anOutputs.length);
        Assert.assertTrue(BytesUtils.isReduceByteArray(anOutputs, n));
        // 两次编码结果应该不一致
        Assert.assertNotEquals(ByteBuffer.wrap(outputs), ByteBuffer.wrap(anOutputs));
    }
}

package edu.alibaba.mpc4j.common.tool.coder;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoder;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoderFactory;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoderFactory.LinearCoderType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
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
 * BCH编码器测试。
 *
 * @author Weiran Liu
 * @date 2021/12/15
 */
@RunWith(Parameterized.class)
public class LinearCoderTest {
    /**
     * 最大编码数量
     */
    private static final int MAX_CODE_NUM = 1 << Byte.SIZE;
    /**
     * 测试线性关系的数据字数量
     */
    private static final int LINEARITY_DATAWORD_NUM = 400;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configuration = new ArrayList<>();
        // HADAMARD_008_256
        configuration.add(new Object[] {LinearCoderType.REPUTATION_001_128.name(), LinearCoderType.REPUTATION_001_128, });
        // HADAMARD_008_256
        configuration.add(new Object[] {LinearCoderType.HADAMARD_008_256.name(), LinearCoderType.HADAMARD_008_256, });
        // BCH_065_448
        configuration.add(new Object[] {LinearCoderType.BCH_065_448.name(), LinearCoderType.BCH_065_448, });
        // BCH_072_462
        configuration.add(new Object[] {LinearCoderType.BCH_072_462.name(), LinearCoderType.BCH_072_462, });
        // BCH_076_511
        configuration.add(new Object[] {LinearCoderType.BCH_076_511.name(), LinearCoderType.BCH_076_511, });
        // BCH_084_495
        configuration.add(new Object[] {LinearCoderType.BCH_084_495.name(), LinearCoderType.BCH_084_495, });
        // BCH_090_495
        configuration.add(new Object[] {LinearCoderType.BCH_090_495.name(), LinearCoderType.BCH_090_495, });
        // BCH_132_583
        configuration.add(new Object[] {LinearCoderType.BCH_132_583.name(), LinearCoderType.BCH_132_583, });
        // BCH_138_594
        configuration.add(new Object[] {LinearCoderType.BCH_138_594.name(), LinearCoderType.BCH_138_594, });
        // BCH_144_605
        configuration.add(new Object[] {LinearCoderType.BCH_144_605.name(), LinearCoderType.BCH_144_605, });
        // BCH_150_616
        configuration.add(new Object[] {LinearCoderType.BCH_150_616.name(), LinearCoderType.BCH_150_616, });
        // BCH_156_627
        configuration.add(new Object[] {LinearCoderType.BCH_156_627.name(), LinearCoderType.BCH_156_627, });
        // BCH_162_638
        configuration.add(new Object[] {LinearCoderType.BCH_162_638.name(), LinearCoderType.BCH_162_638, });
        // BCH_168_649
        configuration.add(new Object[] {LinearCoderType.BCH_168_649.name(), LinearCoderType.BCH_168_649, });
        // BCH_174_660
        configuration.add(new Object[] {LinearCoderType.BCH_174_660.name(), LinearCoderType.BCH_174_660, });
        // BCH_210_732
        configuration.add(new Object[] {LinearCoderType.BCH_210_732.name(), LinearCoderType.BCH_210_732, });
        // BCH_217_744
        configuration.add(new Object[] {LinearCoderType.BCH_217_744.name(), LinearCoderType.BCH_217_744, });
        // BCH_231_768
        configuration.add(new Object[] {LinearCoderType.BCH_231_768.name(), LinearCoderType.BCH_231_768, });
        // BCH_238_776
        configuration.add(new Object[] {LinearCoderType.BCH_238_776.name(), LinearCoderType.BCH_238_776, });

        return configuration;
    }

    /**
     * 待测试的线性编码
     */
    private final LinearCoder linearCoder;
    /**
     * 测试数据码数量
     */
    private final int datawordNum;

    public LinearCoderTest(String name, LinearCoderType linearCoderType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        linearCoder = LinearCoderFactory.getInstance(linearCoderType);
        datawordNum = Math.min(MAX_CODE_NUM, 1 << linearCoder.getDatawordBitLength());
    }

    @Test
    public void testEncode() {
        byte[][] datawords = IntStream.range(0, datawordNum)
            .mapToObj(dataword -> IntUtils.nonNegIntToFixedByteArray(dataword, linearCoder.getDatawordByteLength()))
            .toArray(byte[][]::new);
        byte[][] codewords = Arrays.stream(datawords)
            .map(linearCoder::encode)
            .toArray(byte[][]::new);
        // 验证码字长度
        Arrays.stream(codewords).forEach(codeword -> {
            Assert.assertEquals(linearCoder.getCodewordByteLength(), codeword.length);
            Assert.assertTrue(BytesUtils.isReduceByteArray(codeword, linearCoder.getCodewordBitLength()));
        });
        // 验证汉明距离
        for (int i = 0; i < codewords.length; i++) {
            for (int j = i + 1; j < codewords.length; j++) {
                int distance = BytesUtils.hammingDistance(codewords[i], codewords[j]);
                Assert.assertTrue(linearCoder.getMinimalHammingDistance() <= distance);
            }
        }
    }

    @Test
    public void testParallel() {
        byte[][] datawords = IntStream.range(0, datawordNum)
            .mapToObj(index -> IntUtils.nonNegIntToFixedByteArray(0, linearCoder.getDatawordByteLength()))
            .toArray(byte[][]::new);
        Set<ByteBuffer> codewordSet = Arrays.stream(datawords)
            .parallel()
            .map(linearCoder::encode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, codewordSet.size());
    }

    @Test
    public void testLinearity() {
        // 选择一部分编码，互相进行异或运算，再计算编码结果的异或值，验证是否相等
        byte[][] datawords = IntStream.range(0, LINEARITY_DATAWORD_NUM)
            .mapToObj(index -> {
                byte[] dataword = new byte[linearCoder.getDatawordByteLength()];
                SECURE_RANDOM.nextBytes(dataword);
                BytesUtils.reduceByteArray(dataword, linearCoder.getDatawordBitLength());
                return dataword;
            })
            .toArray(byte[][]::new);
        byte[][] codewords = Arrays.stream(datawords)
            .map(linearCoder::encode)
            .toArray(byte[][]::new);
        // 验证各个异或结果是否满足线性特性
        for (int i = 0; i < datawords.length; i++) {
            for (int j = i; j < datawords.length; j++) {
                byte[] codeword0 = codewords[i];
                byte[] codeword1 = codewords[j];
                byte[] xorEncode = linearCoder.encode(BytesUtils.xor(datawords[i], datawords[j]));
                byte[] encodeXor = BytesUtils.xor(codeword0, codeword1);
                Assert.assertArrayEquals(encodeXor, xorEncode);
            }
        }
    }
}

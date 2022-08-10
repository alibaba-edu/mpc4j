package edu.alibaba.mpc4j.common.tool.coder;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.coder.linear.ReputationCoder;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 重复编码器测试。
 *
 * @author Weiran Liu
 * @date 2021/12/14
 */
@RunWith(Parameterized.class)
public class ReputationCoderTest {
    /**
     * 并发编码数量
     */
    private static final int PARALLEL_ENCODE_NUM = Byte.MAX_VALUE;

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // 码字长度 = 1
        configurationParams.add(new Object[] {"codeword length = 1", new ReputationCoder(1), });
        // 码字长度 = 7
        configurationParams.add(new Object[] {"codeword length = 7", new ReputationCoder(7), });
        // 码字长度 = 40
        configurationParams.add(new Object[] {"codeword length = 40", new ReputationCoder(40), });
        // 码字长度 = 128
        configurationParams.add(new Object[] {"codeword length = 128", new ReputationCoder(128), });

        return configurationParams;
    }

    /**
     * 待测试的哈达码编码
     */
    private final ReputationCoder coder;

    public ReputationCoderTest(String name, ReputationCoder coder) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.coder = coder;
    }

    @Test
    public void testEncode() {
        // 验证0的编码结果
        byte[] falseDataword = IntUtils.nonNegIntToFixedByteArray(0, 1);
        byte[] falseCodeword = coder.encode(falseDataword);
        Assert.assertEquals(coder.getCodewordByteLength(), falseCodeword.length);
        Assert.assertTrue(BytesUtils.isReduceByteArray(falseCodeword, 1));
        // 验证1的编码结果
        byte[] trueDataword = IntUtils.nonNegIntToFixedByteArray(1, 1);
        byte[] trueCodeword = coder.encode(trueDataword);
        Assert.assertEquals(coder.getCodewordByteLength(), trueCodeword.length);
        Assert.assertTrue(BytesUtils.isReduceByteArray(trueCodeword, coder.getCodewordBitLength()));
        // 验证汉明距离
        int hammingDistance = BytesUtils.hammingDistance(falseCodeword, trueCodeword);
        Assert.assertEquals(coder.getMinimalHammingDistance(), hammingDistance);
    }

    @Test
    public void testParallel() {
        byte[][] datawords = IntStream.range(0, PARALLEL_ENCODE_NUM)
            .mapToObj(index -> IntUtils.nonNegIntToFixedByteArray(0, 1))
            .toArray(byte[][]::new);
        Set<ByteBuffer> codewordSet = Arrays.stream(datawords)
            .parallel()
            .map(coder::encode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, codewordSet.size());
    }
}

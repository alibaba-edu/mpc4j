package edu.alibaba.mpc4j.common.tool.coder;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.coder.random.RandomCoder;
import edu.alibaba.mpc4j.common.tool.coder.random.RandomCoderUtils;
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
 * 伪随机编码测试。
 *
 * @author Weiran Liu
 * @date 2021/12/20
 */
@RunWith(Parameterized.class)
public class RandomCoderTest {
    /**
     * 最大编码数量
     */
    private static final int MAX_CODE_NUM = 1 << Byte.SIZE;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configuration = new ArrayList<>();
        int minByteLength = RandomCoderUtils.getMinCodewordByteLength();
        int maxByteLength = RandomCoderUtils.getMaxCodewordByteLength();
        for (int codewordByteLength = minByteLength; codewordByteLength <= maxByteLength; codewordByteLength++) {
            // 依次添加待测试的伪随机数编码
            configuration.add(new Object[] {"l = " + codewordByteLength, codewordByteLength, });
        }
        return configuration;
    }

    /**
     * 码字字节长度
     */
    private final int codewordByteLength;

    public RandomCoderTest(String name, int codewordByteLength) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.codewordByteLength = codewordByteLength;
    }

    @Test
    public void testEncode() {
        RandomCoder randomCoder = new RandomCoder(EnvType.STANDARD, codewordByteLength);
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(key);
        randomCoder.setKey(key);
        byte[][] datawords = IntStream.range(0, MAX_CODE_NUM)
            .mapToObj(IntUtils::intToByteArray)
            .toArray(byte[][]::new);
        byte[][] codewords = Arrays.stream(datawords)
            .map(randomCoder::encode)
            .toArray(byte[][]::new);
        // 验证码字长度
        Arrays.stream(codewords).forEach(codeword -> {
            Assert.assertEquals(randomCoder.getCodewordByteLength(), codeword.length);
            Assert.assertTrue(BytesUtils.isReduceByteArray(codeword, randomCoder.getCodewordBitLength()));
        });
        // 验证汉明距离
        for (int i = 0; i < codewords.length; i++) {
            for (int j = i + 1; j < codewords.length; j++) {
                int distance = BytesUtils.hammingDistance(codewords[i], codewords[j]);
                Assert.assertTrue(randomCoder.getMinimalHammingDistance() <= distance);
            }
        }
    }

    @Test
    public void testParallel() {
        RandomCoder randomCoder = new RandomCoder(EnvType.STANDARD, codewordByteLength);
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(key);
        randomCoder.setKey(key);
        byte[][] datawords = IntStream.range(0, MAX_CODE_NUM)
            .mapToObj(index -> IntUtils.intToByteArray(0))
            .toArray(byte[][]::new);
        Set<ByteBuffer> codewordSet = Arrays.stream(datawords)
            .parallel()
            .map(randomCoder::encode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, codewordSet.size());
    }
}

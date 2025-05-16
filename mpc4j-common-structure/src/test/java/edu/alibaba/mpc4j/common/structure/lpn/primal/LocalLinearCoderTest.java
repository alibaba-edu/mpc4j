package edu.alibaba.mpc4j.common.structure.lpn.primal;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * d-本地线性编码测试。
 *
 * @author Weiran Liu
 * @date 2022/01/31
 */
@RunWith(Parameterized.class)
public class LocalLinearCoderTest {
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();

        // 论文的初始化LPN参数：k = 37248, n = 616092
        configurationParams.add(new Object[]{"k = 37248, n = 616092", 37248, 616092});
        // 论文的迭代LPN参数：k = 588160, n = 10616092
        configurationParams.add(new Object[]{"k = 588160, n = 10616092", 588160, 616092});

        return configurationParams;
    }

    /**
     * 输入数量
     */
    private final int k;
    /**
     * 输出数量
     */
    private final int n;

    public LocalLinearCoderTest(String name, int k, int n) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.k = k;
        this.n = n;
        secureRandom = new SecureRandom();
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
        // init coder with random seed
        byte[] seed = BlockUtils.randomBlock(secureRandom);
        LocalLinearCoder localLinearCoder = new LocalLinearCoder(k, n, seed);
        localLinearCoder.setParallel(parallel);
        // encode different byte length
        for (int byteL = 1; byteL <= CommonConstants.BLOCK_BYTE_LENGTH; byteL = byteL << 1) {
            // random input
            byte[][] e = BytesUtils.randomByteArrayVector(k, byteL, secureRandom);
            byte[][] w1 = localLinearCoder.encode(e);
            byte[][] w2 = localLinearCoder.encode(e);
            // two encode output should be equal
            Assert.assertArrayEquals(w1, w2);
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
        // init coder with random seed
        byte[] seed = BlockUtils.randomBlock(secureRandom);
        LocalLinearCoder localLinearCoder = new LocalLinearCoder(k, n, seed);
        localLinearCoder.setParallel(parallel);
        // random input
        boolean[] e = BinaryUtils.randomBinary(k, secureRandom);
        boolean[] w1 = localLinearCoder.encode(e);
        boolean[] w2 = localLinearCoder.encode(e);
        // two encode output should be equal
        Assert.assertArrayEquals(w1, w2);
    }
}

package edu.alibaba.mpc4j.common.tool.coder;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.coder.linear.HadamardCoder;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
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
 * Hadamard coder test.
 *
 * @author Weiran Liu
 * @date 2021/12/14
 */
@RunWith(Parameterized.class)
public class HadamardCoderTest {
    /**
     * maximal number of datawords used in the test.
     */
    private static final int MAX_CODE_NUM = 1 << Byte.SIZE;

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        // k = 1
        configurations.add(new Object[] {"k = 1", 1, });
        // k = 2
        configurations.add(new Object[] {"k = 2", 2, });
        // k = 3
        configurations.add(new Object[] {"k = 3", 3, });
        // k = 4
        configurations.add(new Object[] {"k = 4", 4, });
        // k = 7
        configurations.add(new Object[] {"k = 7", 7, });
        // k = 8
        configurations.add(new Object[] {"k = 8", 8, });
        // k = 10
        configurations.add(new Object[] {"k = 10", 10, });

        return configurations;
    }

    /**
     * the input bit length k
     */
    private final int k;
    /**
     * the output bit length n
     */
    private final int n;
    /**
     * the Hadamard coder
     */
    private final HadamardCoder coder;
    /**
     * the number of datawords used in the tests.
     */
    private final int datawordNum;

    public HadamardCoderTest(String name, int k) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        coder = new HadamardCoder(k);
        this.k = k;
        n = 1 << k;
        datawordNum = Math.min(MAX_CODE_NUM, 1 << coder.getDatawordBitLength());
    }

    @Test
    public void testParams() {
        // dataword bit length = k
        Assert.assertEquals(k, coder.getDatawordBitLength());
        // codeword bit length = n
        Assert.assertEquals(n, coder.getCodewordBitLength());
        // hamming distance = n / 2
        Assert.assertEquals(n / 2, coder.getMinimalHammingDistance());
    }

    @Test
    public void testEncode() {
        byte[][] datawords = IntStream.range(0, datawordNum)
            .mapToObj(dataword -> IntUtils.nonNegIntToFixedByteArray(dataword, coder.getDatawordByteLength()))
            .toArray(byte[][]::new);
        byte[][] codewords = Arrays.stream(datawords)
            .map(coder::encode)
            .toArray(byte[][]::new);
        // verify the codeword length
        Arrays.stream(codewords).forEach(codeword ->
            Assert.assertTrue(BytesUtils.isFixedReduceByteArray(
                codeword, coder.getCodewordByteLength(), coder.getCodewordBitLength()
            )
        ));
        // verify the hamming distance
        for (int i = 0; i < codewords.length; i++) {
            for (int j = i + 1; j < codewords.length; j++) {
                int distance = BytesUtils.hammingDistance(codewords[i], codewords[j]);
                Assert.assertEquals(coder.getMinimalHammingDistance(), distance);
            }
        }
    }

    @Test
    public void testParallel() {
        byte[][] datawords = IntStream.range(0, datawordNum)
            .mapToObj(index -> IntUtils.nonNegIntToFixedByteArray(0, coder.getDatawordByteLength()))
            .toArray(byte[][]::new);
        Set<ByteBuffer> codewordSet = Arrays.stream(datawords)
            .parallel()
            .map(coder::encode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, codewordSet.size());
    }

    @Test
    public void testIntMul() {
        int[] inputVector, outputVector;
        // all 0 results in 0
        inputVector = new int[n];
        outputVector = HadamardCoder.fastWalshHadamardTrans(inputVector);
        for (int i = 0; i < n; i++) {
            Assert.assertEquals(0, outputVector[i]);
        }
        // all 1 results in n in the first place, and 0 otherwise
        Arrays.fill(inputVector, 1);
        outputVector = HadamardCoder.fastWalshHadamardTrans(inputVector);
        Assert.assertEquals(n, outputVector[0], DoubleUtils.PRECISION);
        for (int i = 1; i < n; i++) {
            Assert.assertEquals(0, outputVector[i]);
        }
        // all -1 results in -n in the first place, and 0 otherwise
        Arrays.fill(inputVector, -1);
        outputVector = HadamardCoder.fastWalshHadamardTrans(inputVector);
        Assert.assertEquals(-n, outputVector[0], DoubleUtils.PRECISION);
        for (int i = 1; i < n; i++) {
            Assert.assertEquals(0, outputVector[i]);
        }
    }

    @Test
    public void testDoubleMul() {
        double[] inputVector, outputVector;
        // all 0 results in 0
        inputVector = new double[n];
        outputVector = HadamardCoder.fastWalshHadamardTrans(inputVector);
        for (int i = 0; i < n; i++) {
            Assert.assertEquals(0, outputVector[i], DoubleUtils.PRECISION);
        }
        // all 1 results in n in the first place, and 0 otherwise
        Arrays.fill(inputVector, 1);
        outputVector = HadamardCoder.fastWalshHadamardTrans(inputVector);
        Assert.assertEquals(n, outputVector[0], DoubleUtils.PRECISION);
        for (int i = 1; i < n; i++) {
            Assert.assertEquals(0, outputVector[i], DoubleUtils.PRECISION);
        }
        // all -1 results in -n in the first place, and 0 otherwise
        Arrays.fill(inputVector, -1);
        outputVector = HadamardCoder.fastWalshHadamardTrans(inputVector);
        Assert.assertEquals(-n, outputVector[0], DoubleUtils.PRECISION);
        for (int i = 1; i < n; i++) {
            Assert.assertEquals(0, outputVector[i], DoubleUtils.PRECISION);
        }
    }

    @Test
    public void testIntInplaceMul() {
        int[] vector;
        // all 0 results in 0
        vector = new int[n];
        HadamardCoder.inplaceFastWalshHadamardTrans(vector);
        for (int i = 0; i < n; i++) {
            Assert.assertEquals(0, vector[i]);
        }
        // all 1 results in n in the first place, and 0 otherwise
        Arrays.fill(vector, 1);
        HadamardCoder.inplaceFastWalshHadamardTrans(vector);
        Assert.assertEquals(n, vector[0], DoubleUtils.PRECISION);
        for (int i = 1; i < n; i++) {
            Assert.assertEquals(0, vector[i]);
        }
        // all -1 results in -n in the first place, and 0 otherwise
        Arrays.fill(vector, -1);
        HadamardCoder.inplaceFastWalshHadamardTrans(vector);
        Assert.assertEquals(-n, vector[0]);
        for (int i = 1; i < n; i++) {
            Assert.assertEquals(0, vector[i]);
        }
    }

    @Test
    public void testDoubleInplaceMul() {
        double[] vector;
        // all 0 results in 0
        vector = new double[n];
        HadamardCoder.inplaceFastWalshHadamardTrans(vector);
        for (int i = 0; i < n; i++) {
            Assert.assertEquals(0, vector[i], DoubleUtils.PRECISION);
        }
        // all 1 results in n in the first place, and 0 otherwise
        Arrays.fill(vector, 1);
        HadamardCoder.inplaceFastWalshHadamardTrans(vector);
        Assert.assertEquals(n, vector[0], DoubleUtils.PRECISION);
        for (int i = 1; i < n; i++) {
            Assert.assertEquals(0, vector[i], DoubleUtils.PRECISION);
        }
        // all -1 results in -n in the first place, and 0 otherwise
        Arrays.fill(vector, -1);
        HadamardCoder.inplaceFastWalshHadamardTrans(vector);
        Assert.assertEquals(-n, vector[0], DoubleUtils.PRECISION);
        for (int i = 1; i < n; i++) {
            Assert.assertEquals(0, vector[i], DoubleUtils.PRECISION);
        }
    }

    @Test
    public void testCheckParity() {
        boolean[][] hadamardMatrix = HadamardCoder.createHadamardMatrix(k);
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                Assert.assertEquals(hadamardMatrix[x][y], HadamardCoder.checkParity(x, y));
            }
        }
    }
}

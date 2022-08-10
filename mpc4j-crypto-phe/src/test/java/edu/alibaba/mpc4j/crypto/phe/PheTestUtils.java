package edu.alibaba.mpc4j.crypto.phe;

import edu.alibaba.mpc4j.crypto.phe.params.PhePlaintext;
import edu.alibaba.mpc4j.crypto.phe.params.PhePlaintextEncoder;
import org.junit.Assert;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * 半同态加密测试工具。
 *
 * @author Weiran Liu
 * @date 2021/12/24
 */
public class PheTestUtils {
    /**
     * 误差精度默认值
     */
    public static final double EPSILON = 1e-3;
    /**
     * 随机化测试的最大迭代次数
     */
    public static final int MAX_ITERATIONS = 40;
    /**
     * 默认安全等级
     */
    public static final PheSecLevel DEFAULT_PHE_SEC_LEVEL = PheSecLevel.LAMBDA_40;
    /**
     * 随机状态
     */
    public static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PheTestUtils() {
        // empty
    }

    /**
     * 返回一个随机{@code double}。
     *
     * @return 随机{@code double}。
     */
    public static double randomDouble() {
        return Double.longBitsToDouble(SECURE_RANDOM.nextLong());
    }

    /**
     * 返回一个不为无穷大的随机{@code double}。
     *
     * @return 不为无穷大的随机{@code double}。
     */
    public static double randomFiniteDouble() {
        while (true) {
            double value = randomDouble();
            if (!(Double.isInfinite(value) || Double.isNaN(value))) {
                return value;
            }
        }
    }

    /**
     * 返回一个未定义的随机{@code double}。即：表示方法不满足浮点数定义标准。
     *
     * @return 未定义的随机{@code double}。
     */
    public static double randomNaNDouble() {
        while (true) {
            // Generate a random NaN/infinity
            double value = Double.longBitsToDouble(0x7FF000000000000L | SECURE_RANDOM.nextLong());
            if (Double.isNaN(value)) {
                return value;
            }
        }
    }

    /**
     * 返回一个随机的且可能为负的{@code BigInteger}。
     *
     * @param bitLength 比特长度。
     * @return 随机的且可能为负的{@code BigInteger}。
     */
    public static BigInteger randomBigInteger(int bitLength) {
        BigInteger value = new BigInteger(bitLength, SECURE_RANDOM);
        int signInt = SECURE_RANDOM.nextInt(2);
        if (signInt % 2 == 0) {
            return value;
        } else {
            return value.negate();
        }
    }

    /**
     * 验证输入数据可被编码。
     *
     * @param encodeScheme 编码方案。
     * @param number       编码数。
     */
    public static void testEncodable(PhePlaintextEncoder encodeScheme, PhePlaintext number) {
        Assert.assertTrue(encodeScheme.isValid(number));
    }

    /**
     * 验证输入数据不可被编码。
     *
     * @param encodeScheme 编码方案。
     * @param number       编码数。
     */
    public static void testUnencodable(PhePlaintextEncoder encodeScheme, BigInteger number) {
        try {
            encodeScheme.encode(number);
            Assert.fail("ERROR: Should not be able to encode number.");
        } catch (CryptoEncodeException ignored) {

        }
    }

    /**
     * 验证输入数据不可被编码。
     *
     * @param encodeScheme  编码方案。
     * @param encodedNumber 编码数。
     */
    public static void testUndecodable(PhePlaintextEncoder encodeScheme, PhePlaintext encodedNumber) {
        try {
            encodeScheme.decodeDouble(encodedNumber);
            Assert.fail("ERROR: successfully decode invalid number.");
        } catch (CryptoDecodeException | ArithmeticException ignored) {

        }
    }
}

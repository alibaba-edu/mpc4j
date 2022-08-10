/*
 * Original Work Copyright 2013 Square Inc.
 * Modified by Weiran Liu. Adjust the code based on Alibaba Java Code Guidelines.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.alibaba.mpc4j.common.jnagmp;

import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static edu.alibaba.mpc4j.common.jnagmp.Gmp.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests {@link Gmp}.
 *
 * @author Square Inc.
 */
public class GmpTest implements AutoCloseable {
    /**
     * 测试向量1
     */
    private static final TestVector VECTOR1 = new TestVector(
        "cc4a529f5c5fbb7afaeb3ccea4a023cb941d2218fb6565b57817379a33c992e5336190283221b6ed"
            + "8b80a08630c343caea7d3575035b6eceb61a522db8a6b17",
        "d3a2f7f3d5c9665a7c2b75fd4dad9b5f3a64534654c3256d739838e82c296695",
        "5e2213227f2d64f426a8c37e381b102f31323d8fa0420ef2c5b12066acae47b9",
        "8d174ff7e3db9991a81cf953891e6794d1983784388218f3a2657b4572c64463",
        "cd2ee1d9211fc1d945dacb2114fdbfa5d5b1b72e20e5ef07b2da5f447a671b87",
        "88c9ebe616152be62e91dcc0b8a92a6e8e767a1ec0994a0521e6ea2da6ef67af",
        "8e3925217bbc8361f6c2c880de852aad0bfdc490d835b2298990e3f280ba0f06");
    /**
     * 测试向量2
     */
    private static final TestVector VECTOR2 = new TestVector(
        "311ca15097932b90881055205851ebea033220c07eb5577a5a92947df7efd3e44369f009e5255291"
            + "78e1f84004b928fd097473c50dff2d43596ab25d328bac2bed52ecb9e567c923fc4d4f969696748d"
            + "807e5179cd7989a5edc2bbc52e6c3b476934c62a4a7e727ad1382c37ecc026d88c813757e230aea9"
            + "18a41ec2938fd221",
        "f66ae786614f80628645e7439fef37e89fff602b95131c83cdc0c208edcded04d8ae6d8acc4b095f"
            + "1d2825e5ba9ceb5b1f1baf886f6a422a97c3d982cb390aed",
        "929ae2cbdece33de8cf4a36cc267513f8bfd8b9be81c2710697751ef4029598dfa4d392947b7422f"
            + "8c727f28ed16a71a005381b108bcde605a34a92a7f3a261d",
        "a447450440dfaaec5983ef826a9f7a9b1554eac7b8b76857de80815b4933f3589074490732dcb0ea"
            + "13701943d1bdf23cbf67ca5af4f18171ba829101dcd0b1f3",
        "b68048b372f5f920e482d1263668d7217c3c5e22dd8b1b8aaad2696cc2fd4f8203555e3c3926cfef"
            + "01a502b22be86178f4117e1641de376fbc803d16fff796a5",
        "79aadb224ca3fb6b4301e0c42445e4c0fd7d94173e5cbd071c8c464881fe3501578e3ed2d0c48a9f"
            + "566e01cc1d459650a2b6540ed69424f528557e0f554fb9c3",
        "78ac8f1b6e9859bcf76f1e536ba2c9f1da2be1a464166cda2f8c1a9f0f31c3d725e11e659c8201b0"
            + "c067cd0feffdd61f7f878adc5039cf0e50fb86f97effd3a2");
    /**
     * 测试向量3
     */
    private static final TestVector VECTOR3 = new TestVector(
        "4980b0d6e5e5281e698f8894988e78f4f9c8cc6eecae3b7f62b4d017d345fe7a29ff6b84434a9b6a"
            + "563ffca8458d11b22395f5c6ad1bb9ec92e5f6d1955cd870cf1f8e00b7f10a66e18c2e46611fd93e"
            + "1eba9c2cff851f7a54d0a1a5c72411296ca08ae046461022a507c1af6c9f67c60f3a75f05b2f125f"
            + "4dfc7323230ebae288be04967b0940e734c187562e3707d2d850538fbf8b10e2eeb8d45e49ca254c"
            + "11a7378e6e1d194d49b4cea5496a583b7d31e474e5d3a655b31bfbf85203b6e894b47e429fd87025"
            + "5b56049cfdd58ed203e6a39e4783f17c5400d02b4a63c8275e3af69ad26b6e2f5415773d2fddcb6f"
            + "53bdb9078c68bf6f9d351e96bacc7b5b",
        "ef51701631cdda9d8d6616b29024fca8ec1b9511ffa8d437c6bd6b25748b66a1884bbecdb3df13d6"
            + "c125f4a5ce0ec0f2c6e90deeb31d8abd7b00921efe6bc19b65eaca66146b98d732a98d7e18d1379e"
            + "bc72ebc8bd343ce8ca469a4d63c562ac236b4f89af14102bed2c230062d19f9e04fd742a1e124fe0"
            + "77d3841bb4ba0d63",
        "3d0d6bafedeb3774c92856eba1a019ed51dad2bf009e0ace835bb87799e4992409bd2f7bea208331"
            + "0ac88e220d27cdd83e38eb30ba95724d0752df9b324ca0591e00d159c882c7987b580df3708a9182"
            + "8bbda82aaeffa7311cebb8eaa82591c611c792458b35ed9d56f466db64491033248de1d65caa0229"
            + "c1f97e4d1f4633f8",
        "9f8ba00ecbde91be5e440f21b56dfdc5f2bd0e0bffc5e2cfd9d39cc3a30799c10587d4892294b7e4"
            + "80c3f86e895f2b4c849b5e9f2213b1d3a755b6bf5447d6679947319962f265e4cc7108febb362514"
            + "7da1f285d378289b318466de4283971d6cf2350674b80ac7f372c20041e115140353a2c6beb6dfea"
            + "fa8d02bd2326b397",
        "9f7cb37e05714d994d4b3a1c38e4a16d51f27843080f0b6ccd59b6101f6fcc4ad066b7ba320b0e6b"
            + "dce570a6bec1e59ca771302725393ebf0a263518e8c287f3c7fd55a29b5a99aa85c5ab27c319c874"
            + "32b61843cc347f098cf83cba2b8113a95c9c84aedb7b976e4ff8be5b06341de933ca51c0a3163e85"
            + "1e41575de4da8b99",
        "6a53225403a0de6633877c12d0986b9e36a1a582055f5cf33391240abf9fdd873599cfd176b2099d"
            + "3dee4b19d48143bdc4f6201a18d0d47f5c1978bb45d7054d2ffe3917123c6671ae83c76fd76685a2"
            + "cc7965828822ff5bb350287c1d00b7c63dbdadc9e7a7ba498aa5d43caecd69462286e12b176429ae"
            + "142b8f93ede707bb",
        "64ad901529625b9ab8a0f37224b7e72c6c36906c06b7f488c7d8eaf886a7577a75107d67b089cff7"
            + "0e9dfeba9794b64fbeea7711098c752b4765adc199a638bc73a0b6d1f96adf4ab17ac3fd5d42c994"
            + "20730843b2a4e2e597b39e228b819a59b89a934a34de0ec948b396d738010aaeaaa5c11b9335ff0b"
            + "b06a53c04743c38d");

    /**
     * Java的modPow
     */
    private static final ModPowStrategy JAVA = BigInteger::modPow;
    /**
     * GMP的不安全modPow
     */
    private static final ModPowStrategy INSECURE = Gmp::modPowInsecure;
    /**
     * GMP封装成GmpInteger的不安全modPow
     */
    private static final ModPowStrategy INSECURE_GMP_INTS = (base, exponent, modulus)
        -> modPowInsecure(new GmpBigInteger(base), new GmpBigInteger(exponent), new GmpBigInteger(modulus));
    /**
     * GMP的安全modPow
     */
    private static final ModPowStrategy SECURE = Gmp::modPowSecure;
    /**
     * GMP封装成GmpInteger的安全modPow
     */
    private static final ModPowStrategy SECURE_GMP_INTS = (base, exponent, modulus)
        -> modPowSecure(new GmpBigInteger(base), new GmpBigInteger(exponent), new GmpBigInteger(modulus));

    @BeforeClass
    public static void checkLoaded() {
        Gmp.checkLoaded();
    }

    @Override
    public void close() throws Exception {
        Gmp.INSTANCE.remove();
        final AtomicBoolean gcHappened = new AtomicBoolean(false);
        gcHappened.set(true);
        while (!gcHappened.get()) {
            System.gc();
            //noinspection BusyWait
            Thread.sleep(100);
        }
    }

    /**
     * 模指数运算策略，通过不同的实现选择出不同的策略
     */
    interface ModPowStrategy {
        BigInteger modPow(BigInteger base, BigInteger exponent, BigInteger modulus);
    }

    /**
     * 当前测试的modPow策略
     */
    private ModPowStrategy strategy;

    private long modPow(long base, long exponent, long modulus) {
        return strategy.modPow(BigInteger.valueOf(base), BigInteger.valueOf(exponent),
            BigInteger.valueOf(modulus)).longValue();
    }

    @Test
    public void testExamplesJava() {
        strategy = JAVA;
        testOddExamples();
        testEvenExamples();
    }

    @Test
    public void testExamplesInsecure() {
        strategy = INSECURE;
        testOddExamples();
        testEvenExamples();
    }

    @Test
    public void testExamplesInsecureGmpInts() {
        strategy = INSECURE_GMP_INTS;
        testOddExamples();
        testEvenExamples();
    }

    @Test
    public void testExamplesSecure() {
        strategy = SECURE;
        testOddExamples();
    }

    @Test
    public void testExamplesSecureGmpInts() {
        strategy = SECURE_GMP_INTS;
        testOddExamples();
    }

    /**
     * 测试模数为奇数的情况，即验证2^3 mod 3, 2^3 mod 5, 2^3 mod 7, 2^3 mod 9计算结果是否正确。
     */
    private void testOddExamples() {
        // 2 ^ 3 = 8
        assertEquals(2, modPow(2, 3, 3));
        assertEquals(3, modPow(2, 3, 5));
        assertEquals(1, modPow(2, 3, 7));
        assertEquals(8, modPow(2, 3, 9));
    }

    /**
     * 测试模数为偶数的情况，，即验证2^3 mod 2, 2^3 mod 4, 2^3 mod 6, 2^3 mod 8计算结果是否正确。
     */
    private void testEvenExamples() {
        // 2 ^ 3 = 8
        assertEquals(0, modPow(2, 3, 2));
        assertEquals(0, modPow(2, 3, 4));
        assertEquals(2, modPow(2, 3, 6));
        assertEquals(0, modPow(2, 3, 8));
    }

    @Test
    public void testExactDivide() {
        assertEquals(BigInteger.valueOf(1), exactDivide(BigInteger.valueOf(1), BigInteger.valueOf(1)));
        assertEquals(BigInteger.valueOf(3), exactDivide(BigInteger.valueOf(9), BigInteger.valueOf(3)));
        assertEquals(BigInteger.valueOf(4), exactDivide(BigInteger.valueOf(12), BigInteger.valueOf(3)));
        Random rnd = new Random();
        for (int i = 0; i < 100; i++) {
            BigInteger a = new BigInteger(1024, rnd);
            BigInteger b = new BigInteger(1024, rnd);
            assertEquals(a.gcd(b), gcd(a, b));
        }
    }

    @Test
    public void testExactDivideArithmeticException() {
        try {
            exactDivide(BigInteger.ONE, BigInteger.ZERO);
            fail("ArithmeticException expected");
        } catch (ArithmeticException expected) {
        }
    }

    @Test
    public void testExactDivideSmallExhaustive() {
        for (int a = 10; a >= -10; --a) {
            for (int b = 10; b >= -10; --b) {
                int p = a * b;
                BigInteger aVal = BigInteger.valueOf(a);
                BigInteger bVal = BigInteger.valueOf(b);
                BigInteger pVal = BigInteger.valueOf(p);

                // exactDivide should work both ways
                if (a != 0) {
                    assertEquals(String.format("a %d, b %d, p %d", a, b, p), bVal, exactDivide(pVal, aVal));
                } else {
                    try {
                        exactDivide(pVal, aVal);
                        fail("ArithmeticException expected");
                    } catch (ArithmeticException expected) {
                    }
                }
            }
        }
    }

    @Test
    public void testGcd() {
        assertEquals(BigInteger.valueOf(11), gcd(BigInteger.valueOf(99), BigInteger.valueOf(88)));
        assertEquals(BigInteger.valueOf(4), gcd(BigInteger.valueOf(100), BigInteger.valueOf(88)));
        assertEquals(BigInteger.valueOf(1), gcd(BigInteger.valueOf(101), BigInteger.valueOf(88)));
        Random rnd = new Random();
        for (int i = 0; i < 100; i++) {
            BigInteger a = new BigInteger(1024, rnd);
            BigInteger b = new BigInteger(1024, rnd);
            assertEquals(a.gcd(b), gcd(a, b));
        }
    }

    @Test
    public void testGcdSmallExhaustive() {
        for (int a = 10; a >= -10; --a) {
            for (int b = 10; b >= -10; --b) {
                BigInteger aVal = BigInteger.valueOf(a);
                BigInteger bVal = BigInteger.valueOf(b);
                BigInteger expected = aVal.gcd(bVal);
                BigInteger actual = gcd(aVal, bVal);
                assertEquals(String.format("a %d, b %d", a, b) + b, expected, actual);
            }
        }
    }

    @Test
    public void testModInverse() {
        assertEquals(BigInteger.valueOf(2),
            modInverse(BigInteger.valueOf(3), BigInteger.valueOf(5)));
        Random rnd = new Random();
        BigInteger m = new BigInteger(1024, rnd).nextProbablePrime();
        for (int i = 0; i < 100; i++) {
            BigInteger x = new BigInteger(1023, rnd);
            assertEquals(x.modInverse(m), modInverse(x, m));
        }
    }

    @Test
    public void testModInverseSmallExhaustive() {
        for (int val = 10; val >= 0; --val) {
            for (int mod = 10; mod >= -1; --mod) {
                BigInteger bVal = BigInteger.valueOf(val);
                BigInteger bMod = BigInteger.valueOf(mod);
                try {
                    BigInteger expected = bVal.modInverse(bMod);
                    BigInteger actual = modInverse(bVal, bMod);
                    assertEquals(String.format("val %d, mod %d", val, mod) + mod, expected, actual);
                } catch (ArithmeticException e) {
                    try {
                        modInverse(bVal, bMod);
                        fail("ArithmeticException expected");
                    } catch (ArithmeticException expected) {
                    }
                }
            }
        }
    }

    @Test
    public void testModInverseArithmeticException() {
        try {
            modInverse(BigInteger.ONE, BigInteger.valueOf(-1));
            fail("ArithmeticException expected");
        } catch (ArithmeticException expected) {
        }
        try {
            modInverse(BigInteger.valueOf(3), BigInteger.valueOf(9));
            fail("ArithmeticException expected");
        } catch (ArithmeticException expected) {
        }
    }

    @Test
    public void testKronecker() {
        // Prime (legendre)
        assertEquals(0, kronecker(BigInteger.valueOf(0), BigInteger.valueOf(7)));
        assertEquals(1, kronecker(BigInteger.valueOf(1), BigInteger.valueOf(7)));
        assertEquals(1, kronecker(BigInteger.valueOf(2), BigInteger.valueOf(7)));
        assertEquals(-1, kronecker(BigInteger.valueOf(3), BigInteger.valueOf(7)));
        assertEquals(1, kronecker(BigInteger.valueOf(4), BigInteger.valueOf(7)));
        assertEquals(-1, kronecker(BigInteger.valueOf(5), BigInteger.valueOf(7)));
        assertEquals(-1, kronecker(BigInteger.valueOf(6), BigInteger.valueOf(7)));
        assertEquals(0, kronecker(BigInteger.valueOf(7), BigInteger.valueOf(7)));

        // Non-prime odd (jacobi)
        assertEquals(0, kronecker(BigInteger.valueOf(0), BigInteger.valueOf(9)));
        assertEquals(1, kronecker(BigInteger.valueOf(1), BigInteger.valueOf(9)));
        assertEquals(1, kronecker(BigInteger.valueOf(2), BigInteger.valueOf(9)));
        assertEquals(0, kronecker(BigInteger.valueOf(3), BigInteger.valueOf(9)));
        assertEquals(1, kronecker(BigInteger.valueOf(4), BigInteger.valueOf(9)));
        assertEquals(1, kronecker(BigInteger.valueOf(5), BigInteger.valueOf(9)));
        assertEquals(0, kronecker(BigInteger.valueOf(6), BigInteger.valueOf(9)));
        assertEquals(1, kronecker(BigInteger.valueOf(7), BigInteger.valueOf(9)));
        assertEquals(1, kronecker(BigInteger.valueOf(8), BigInteger.valueOf(9)));
        assertEquals(0, kronecker(BigInteger.valueOf(9), BigInteger.valueOf(9)));

        // Anything (kronecker)
        assertEquals(0, kronecker(BigInteger.valueOf(0), BigInteger.valueOf(8)));
        assertEquals(1, kronecker(BigInteger.valueOf(1), BigInteger.valueOf(8)));
        assertEquals(0, kronecker(BigInteger.valueOf(2), BigInteger.valueOf(8)));
        assertEquals(-1, kronecker(BigInteger.valueOf(3), BigInteger.valueOf(8)));
        assertEquals(0, kronecker(BigInteger.valueOf(4), BigInteger.valueOf(8)));
        assertEquals(-1, kronecker(BigInteger.valueOf(5), BigInteger.valueOf(8)));
        assertEquals(0, kronecker(BigInteger.valueOf(6), BigInteger.valueOf(8)));
        assertEquals(1, kronecker(BigInteger.valueOf(7), BigInteger.valueOf(8)));
        assertEquals(0, kronecker(BigInteger.valueOf(8), BigInteger.valueOf(8)));

        assertEquals(0, kronecker(BigInteger.valueOf(0), BigInteger.valueOf(-8)));
        assertEquals(1, kronecker(BigInteger.valueOf(1), BigInteger.valueOf(-8)));
        assertEquals(0, kronecker(BigInteger.valueOf(2), BigInteger.valueOf(-8)));
        assertEquals(-1, kronecker(BigInteger.valueOf(3), BigInteger.valueOf(-8)));
        assertEquals(0, kronecker(BigInteger.valueOf(4), BigInteger.valueOf(-8)));
        assertEquals(-1, kronecker(BigInteger.valueOf(5), BigInteger.valueOf(-8)));
        assertEquals(0, kronecker(BigInteger.valueOf(6), BigInteger.valueOf(-8)));
        assertEquals(1, kronecker(BigInteger.valueOf(7), BigInteger.valueOf(-8)));
        assertEquals(0, kronecker(BigInteger.valueOf(8), BigInteger.valueOf(-8)));
    }

    @Test
    public void testSmallExhaustiveInsecure() {
        for (int base = 10; base >= -10; --base) {
            for (int exp = 10; exp >= -10; --exp) {
                for (int mod = 10; mod >= -1; --mod) {
                    this.strategy = JAVA;
                    Object expected;
                    try {
                        expected = modPow(base, exp, mod);
                    } catch (Exception e) {
                        expected = e.getClass();
                    }

                    this.strategy = INSECURE;
                    Object actual;
                    try {
                        actual = modPow(base, exp, mod);
                    } catch (Exception e) {
                        actual = e.getClass();
                    }
                    String message = String.format("base %d, exp %d, mod %d", base, exp, mod);
                    assertEquals(message, expected, actual);
                }
            }
        }
    }

    @Test
    public void testSmallExhaustiveSecure() {
        for (int base = 10; base >= -10; --base) {
            for (int exp = 10; exp >= -10; --exp) {
                for (int mod = 10; mod >= -1; --mod) {
                    this.strategy = JAVA;
                    Object expected;
                    try {
                        expected = modPow(base, exp, mod);
                    } catch (Exception e) {
                        expected = e.getClass();
                    }

                    this.strategy = SECURE;
                    Object actual;
                    try {
                        actual = modPow(base, exp, mod);
                    } catch (Exception e) {
                        actual = e.getClass();
                    }
                    if (mod > 0 && mod % 2 == 0) {
                        // modPowSecure does not support even modulus
                        assertEquals(IllegalArgumentException.class, actual);
                    } else {
                        String message = String.format("base %d, exp %d, mod %d", base, exp, mod);
                        assertEquals(message, expected, actual);
                    }
                }
            }
        }
    }

    @Test
    public void testVectorsJava() {
        strategy = JAVA;
        testVectors();
    }

    @Test
    public void testVectorInsecure() {
        strategy = INSECURE;
        testVectors();
    }

    @Test
    public void testVectorsSecure() {
        strategy = SECURE;
        testVectors();
    }

    private void testVectors() {
        doTest(VECTOR1);
        doTest(VECTOR2);
        doTest(VECTOR3);
    }

    /**
     * 测试向量计算结果。
     *
     * @param v 待测试的向量。
     */
    private void doTest(TestVector v) {
        assertEquals(v.resultP, strategy.modPow(v.base, v.rp, v.p));
        assertEquals(v.resultQ, strategy.modPow(v.base, v.rq, v.q));
        assertNotEquals(v.resultP, strategy.modPow(v.base, v.rp, v.q));
        assertNotEquals(v.resultP, strategy.modPow(v.base, v.rq, v.p));
        assertNotEquals(v.resultQ, strategy.modPow(v.base, v.rp, v.q));
        assertNotEquals(v.resultQ, strategy.modPow(v.base, v.rq, v.p));
    }

    private static void assertNotEquals(Object expected, Object actual) {
        if ((expected == null) != (actual == null)) {
            return;
        }
        if (expected != actual && !expected.equals(actual)) {
            return;
        }
        fail("Expected not equals, was: " + actual);
    }
}

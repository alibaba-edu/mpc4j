package edu.alibaba.mpc4j.common.structure.pgm;

import com.google.common.primitives.UnsignedLong;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * tests for <code>LongIntrinsics</code>.
 *
 * @author Weiran Liu
 * @date 2024/7/28
 */
public class LongIntrinsicsTest {
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public LongIntrinsicsTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testMultiplyHighUnsigned() {
        int round = 10_000;
        for (int i = 0; i < round; i++) {
            long a = secureRandom.nextLong();
            long b = secureRandom.nextLong();
            long high = LongIntrinsics.multiplyHighUnsigned(a, b);
            long low = a * b;
            BigInteger expect = UnsignedLong.fromLongBits(high).bigIntegerValue().shiftLeft(Long.SIZE)
                .add(UnsignedLong.fromLongBits(low).bigIntegerValue());
            BigInteger actual = UnsignedLong.fromLongBits(a).bigIntegerValue()
                .multiply(UnsignedLong.fromLongBits(b).bigIntegerValue());
            Assert.assertEquals(
                "(a, b) = " + Long.toUnsignedString(a) + ", " + Long.toUnsignedString(b),
                expect, actual
            );
        }
    }
}

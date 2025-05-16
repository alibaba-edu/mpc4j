package edu.alibaba.mpc4j.common.structure.lpn;

import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.junit.Assert;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * LPN coder utilities.
 *
 * @author Weiran Liu
 * @date 2024/6/17
 */
public class LpnCoderTestUtils {
    /**
     * private constructor.
     */
    private LpnCoderTestUtils() {
        // empty
    }

    /**
     * Generates R0 array.
     *
     * @param n            n.
     * @param secureRandom random state.
     * @return R0 array.
     */
    public static byte[][] generateR0Array(int n, SecureRandom secureRandom) {
        return BlockUtils.randomBlocks(n, secureRandom);
    }

    /**
     * Generates choices.
     *
     * @param n            n.
     * @param secureRandom random state.
     * @return choice array.
     */
    public static boolean[] generateChoices(int n, SecureRandom secureRandom) {
        return BinaryUtils.randomBinary(n, secureRandom);
    }

    /**
     * Generates Rb array.
     *
     * @param r0Array R0 array.
     * @param delta   Δ.
     * @param choices choices.
     * @return Rb array.
     */
    public static byte[][] generateRbArray(byte[] delta, byte[][] r0Array, boolean[] choices) {
        assert r0Array.length == choices.length;
        int n = r0Array.length;
        return IntStream.range(0, n)
            .mapToObj(i -> {
                if (choices[i]) {
                    return BytesUtils.xor(r0Array[i], delta);
                } else {
                    return BytesUtils.clone(r0Array[i]);
                }
            })
            .toArray(byte[][]::new);
    }

    /**
     * asserts the correctness of encode.
     *
     * @param delta         Δ.
     * @param extendR0Array extend R0 array.
     * @param extendChoices extend choices.
     * @param extendRbArray extend Rb array.
     */
    public static void assertEncode(byte[] delta, byte[][] extendR0Array, boolean[] extendChoices, byte[][] extendRbArray) {
        Assert.assertEquals(extendR0Array.length, extendChoices.length);
        Assert.assertEquals(extendR0Array.length, extendRbArray.length);
        int n = extendR0Array.length;
        for (int i = 0; i < n; i++) {
            if (extendChoices[i]) {
                Assert.assertArrayEquals(extendRbArray[i], BytesUtils.xor(extendR0Array[i], delta));
            } else {
                Assert.assertArrayEquals(extendRbArray[i], extendR0Array[i]);
            }
        }
    }
}

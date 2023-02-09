package edu.alibaba.mpc4j.common.tool.utils;

import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * Integer utilities test.
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
public class IntUtilsTest {
    /**
     * maximal number of iterations
     */
    private static final int MAX_ITERATIONS = 100;
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Test
    public void testInvalidIntByteArray() {
        // convert byte[] with length = 0
        Assert.assertThrows(AssertionError.class, () -> IntUtils.byteArrayToInt(new byte[0]));
        // convert byte[] with length = Integer.BYTES - 1
        Assert.assertThrows(AssertionError.class, () -> IntUtils.byteArrayToInt(new byte[Integer.BYTES - 1]));
        // convert byte[] with length = Integer.BYTES + 1
        Assert.assertThrows(AssertionError.class, () -> IntUtils.byteArrayToInt(new byte[Integer.BYTES + 1]));
    }

    @Test
    public void testIntByteArray() {
        testIntByteArray(0, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,});
        testIntByteArray(1, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,});
        testIntByteArray(-1, new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,});
        testIntByteArray(Integer.MAX_VALUE, new byte[]{(byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,});
        testIntByteArray(Integer.MIN_VALUE, new byte[]{(byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00,});
    }

    private void testIntByteArray(int value, byte[] byteArray) {
        byte[] convertByteArray = IntUtils.intToByteArray(value);
        Assert.assertArrayEquals(byteArray, convertByteArray);
        int convertValue = IntUtils.byteArrayToInt(byteArray);
        Assert.assertEquals(value, convertValue);
    }

    @Test
    public void testInvalidBoundedIntByteArray() {
        // convert negative int
        Assert.assertThrows(AssertionError.class, () -> IntUtils.boundedNonNegIntToByteArray(-1, Byte.MAX_VALUE));
        // convert an int value that is greater than Byte.MAX_VALUE to a byte array with upper bound Byte.MAX_VALUE
        Assert.assertThrows(AssertionError.class, () ->
            IntUtils.boundedNonNegIntToByteArray(Byte.MAX_VALUE + 1, Byte.MAX_VALUE)
        );
        // convert an int value that is greater than Short.MAX_VALUE to a byte array with upper bound Short.MAX_VALUE
        Assert.assertThrows(AssertionError.class, () ->
            IntUtils.boundedNonNegIntToByteArray(Short.MAX_VALUE + 1, Short.MAX_VALUE)
        );
        // convert a byte array for negative Byte to an int
        Assert.assertThrows(AssertionError.class, () ->
            IntUtils.byteArrayToBoundedNonNegInt(new byte[]{(byte) 0xFF}, Byte.MAX_VALUE)
        );
        // convert a byte array for negative Short to an int
        Assert.assertThrows(AssertionError.class, () ->
            IntUtils.byteArrayToBoundedNonNegInt(new byte[]{(byte) 0xF0, (byte) 0x00}, Short.MAX_VALUE)
        );
        // convert an int to a byte array that is greater than the bound
        // for Byte
        Assert.assertThrows(AssertionError.class, () ->
            IntUtils.boundedNonNegIntToByteArray(Byte.MAX_VALUE, Byte.MAX_VALUE - 1)
        );
        Assert.assertThrows(AssertionError.class, () ->
            IntUtils.boundedNonNegIntToByteArray(Byte.MAX_VALUE - 1, Byte.MAX_VALUE - 2)
        );
        // for Short
        Assert.assertThrows(AssertionError.class, () ->
            IntUtils.boundedNonNegIntToByteArray(Short.MAX_VALUE, Short.MAX_VALUE - 1)
        );
        Assert.assertThrows(AssertionError.class, () ->
            IntUtils.boundedNonNegIntToByteArray(Short.MAX_VALUE - 1, Short.MAX_VALUE - 2)
        );
        // for Integer
        Assert.assertThrows(AssertionError.class, () ->
            IntUtils.boundedNonNegIntToByteArray(Integer.MAX_VALUE, Integer.MAX_VALUE - 1)
        );
        Assert.assertThrows(AssertionError.class, () ->
            IntUtils.boundedNonNegIntToByteArray(Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 2)
        );
        // convert a byte array to an int that is greater than the bound
        // for Bytes
        Assert.assertThrows(AssertionError.class, () -> {
            byte[] byteArray = IntUtils.boundedNonNegIntToByteArray(Byte.MAX_VALUE, Byte.MAX_VALUE);
            IntUtils.byteArrayToBoundedNonNegInt(byteArray, Byte.MAX_VALUE - 1);
        });
        Assert.assertThrows(AssertionError.class, () -> {
            byte[] byteArray = IntUtils.boundedNonNegIntToByteArray(Byte.MAX_VALUE - 1, Byte.MAX_VALUE);
            IntUtils.byteArrayToBoundedNonNegInt(byteArray, Byte.MAX_VALUE - 2);
        });
        // for Short
        Assert.assertThrows(AssertionError.class, () -> {
            byte[] byteArray = IntUtils.boundedNonNegIntToByteArray(Short.MAX_VALUE, Short.MAX_VALUE);
            IntUtils.byteArrayToBoundedNonNegInt(byteArray, Short.MAX_VALUE - 1);
        });
        Assert.assertThrows(AssertionError.class, () -> {
            byte[] byteArray = IntUtils.boundedNonNegIntToByteArray(Short.MAX_VALUE - 1, Short.MAX_VALUE);
            IntUtils.byteArrayToBoundedNonNegInt(byteArray, Short.MAX_VALUE - 2);
        });
        // for Integer
        Assert.assertThrows(AssertionError.class, () -> {
            byte[] byteArray = IntUtils.boundedNonNegIntToByteArray(Integer.MAX_VALUE, Integer.MAX_VALUE);
            IntUtils.byteArrayToBoundedNonNegInt(byteArray, Integer.MAX_VALUE - 1);
        });
        Assert.assertThrows(AssertionError.class, () -> {
            byte[] byteArray = IntUtils.boundedNonNegIntToByteArray(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
            IntUtils.byteArrayToBoundedNonNegInt(byteArray, Integer.MAX_VALUE - 2);
        });
    }

    @Test
    public void testConstantBoundedIntByteArray() {
        // convert a byte array that is equal to 0, or equal to the bound
        // for Byte
        testConstantBoundedIntByteArray(0, Byte.MAX_VALUE - 1);
        testConstantBoundedIntByteArray(Byte.MAX_VALUE - 1, Byte.MAX_VALUE - 1);
        testConstantBoundedIntByteArray(0, Byte.MAX_VALUE);
        testConstantBoundedIntByteArray(Byte.MAX_VALUE, Byte.MAX_VALUE);
        // for Short
        testConstantBoundedIntByteArray(0, Short.MAX_VALUE - 1);
        testConstantBoundedIntByteArray(Short.MAX_VALUE - 1, Short.MAX_VALUE - 1);
        testConstantBoundedIntByteArray(0, Short.MAX_VALUE);
        testConstantBoundedIntByteArray(Short.MAX_VALUE, Short.MAX_VALUE);
        // for Integer
        testConstantBoundedIntByteArray(0, Integer.MAX_VALUE - 1);
        testConstantBoundedIntByteArray(Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1);
        testConstantBoundedIntByteArray(0, Integer.MAX_VALUE);
        testConstantBoundedIntByteArray(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    private void testConstantBoundedIntByteArray(int boundInt, int bound) {
        byte[] boundIntByteArray = IntUtils.boundedNonNegIntToByteArray(boundInt, bound);
        Assert.assertEquals(boundInt, IntUtils.byteArrayToBoundedNonNegInt(boundIntByteArray, bound));
    }

    @Test
    public void testBoundedIntByteArray() {
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            // convert around Byte.MAX_VALUE
            int smallByteValue = SECURE_RANDOM.nextInt(Byte.MAX_VALUE / 2 + 1);
            testBoundedIntByteArray(smallByteValue, Byte.MAX_VALUE / 2 + 1);
            int byteValue = SECURE_RANDOM.nextInt(Byte.MAX_VALUE);
            testBoundedIntByteArray(byteValue, Byte.MAX_VALUE);
            int largeByteValue = SECURE_RANDOM.nextInt(Byte.MAX_VALUE * 2 - 1);
            testBoundedIntByteArray(largeByteValue, Byte.MAX_VALUE * 2 - 1);
            // convert around Short.MAX_VALUE
            int smallShortValue = SECURE_RANDOM.nextInt(Short.MAX_VALUE / 2 + 1);
            testBoundedIntByteArray(smallShortValue, Short.MAX_VALUE / 2 + 1);
            int shortValue = SECURE_RANDOM.nextInt(Short.MAX_VALUE);
            testBoundedIntByteArray(shortValue, Short.MAX_VALUE);
            int largeShortValue = SECURE_RANDOM.nextInt(Short.MAX_VALUE * 2 - 1);
            testBoundedIntByteArray(largeShortValue, Short.MAX_VALUE * 2 - 1);
            // convert around Integer.MAX_VALUE
            int smallIntValue = SECURE_RANDOM.nextInt(Integer.MAX_VALUE / 2 + 1);
            testBoundedIntByteArray(smallIntValue, Integer.MAX_VALUE / 2 + 1);
            int intValue = SECURE_RANDOM.nextInt(Integer.MAX_VALUE);
            testBoundedIntByteArray(intValue, Integer.MAX_VALUE);
        }
    }

    private void testBoundedIntByteArray(int value, int bound) {
        byte[] convertByteArray = IntUtils.boundedNonNegIntToByteArray(value, bound);
        // verify the byte length
        if (bound <= Byte.MAX_VALUE) {
            Assert.assertEquals(convertByteArray.length, Byte.BYTES);
        } else if (bound <= Short.MAX_VALUE) {
            Assert.assertEquals(convertByteArray.length, Short.BYTES);
        } else {
            Assert.assertEquals(convertByteArray.length, Integer.BYTES);
        }
        int convertValue = IntUtils.byteArrayToBoundedNonNegInt(convertByteArray, bound);
        Assert.assertEquals(value, convertValue);
    }

    @Test
    public void testInvalidIntFixedByteArray() {
        // convert an int to a byte array with length = 0
        Assert.assertThrows(AssertionError.class, () -> IntUtils.nonNegIntToFixedByteArray(0, 0));
        // convert a negative int to a byte array
        Assert.assertThrows(AssertionError.class, () -> IntUtils.nonNegIntToFixedByteArray(-1, Integer.BYTES));
        // convert an int that is larger than the assigned byte length
        Assert.assertThrows(AssertionError.class, () -> IntUtils.nonNegIntToFixedByteArray(256, 1));
        // convert a byte array with length = 0 to an int
        Assert.assertThrows(AssertionError.class, () -> IntUtils.fixedByteArrayToNonNegInt(new byte[0]));
        // convert a byte array with negative int to an non-negative int
        Assert.assertThrows(AssertionError.class, () ->
            IntUtils.fixedByteArrayToNonNegInt(new byte[]{(byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00})
        );
    }

    @Test
    public void testIntFixedByteArray() {
        // convert 0
        testIntFixedByteArray(0, new byte[]{0x00, 0x00, 0x00, 0x00,});
        testIntFixedByteArray(0, new byte[]{0x00,});
        testIntFixedByteArray(0, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00,});
        // convert positive
        testIntFixedByteArray(1, new byte[]{0x00, 0x00, 0x00, 0x01,});
        testIntFixedByteArray(1, new byte[]{0x01,});
        testIntFixedByteArray(1, new byte[]{0x00, 0x00, 0x00, 0x00, 0x01,});
        // convert max value
        testIntFixedByteArray((1 << Byte.SIZE) - 1, new byte[]{(byte) 0xFF,});
        testIntFixedByteArray((1 << 2 * Byte.SIZE) - 1, new byte[]{(byte) 0xFF, (byte) 0xFF,});
        testIntFixedByteArray((1 << 3 * Byte.SIZE) - 1, new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF,});
        testIntFixedByteArray(Integer.MAX_VALUE, new byte[]{(byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,});
        testIntFixedByteArray(Integer.MAX_VALUE, new byte[]{0x00, (byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,});
    }

    private void testIntFixedByteArray(int value, byte[] byteArray) {
        byte[] convertByteArray = IntUtils.nonNegIntToFixedByteArray(value, byteArray.length);
        Assert.assertArrayEquals(byteArray, convertByteArray);
        int convertValue = IntUtils.fixedByteArrayToNonNegInt(byteArray);
        Assert.assertEquals(value, convertValue);
    }

    @Test
    public void testInvalidIntArrayByteArray() {
        // convert an int array with length = 0 to a byte array
        Assert.assertThrows(AssertionError.class, () -> IntUtils.intArrayToByteArray(new int[0]));
        // convert a byte array with length = 0 to an int array
        Assert.assertThrows(AssertionError.class, () -> IntUtils.byteArrayToIntArray(new byte[0]));
        // convert a byte array with length = Integer.BYTES - 1 to an int array
        Assert.assertThrows(AssertionError.class, () -> IntUtils.byteArrayToIntArray(new byte[Integer.BYTES - 1]));
        // convert a byte array with length = Integer.BYTES + 1 to an int array
        Assert.assertThrows(AssertionError.class, () -> IntUtils.byteArrayToIntArray(new byte[Integer.BYTES + 1]));
        // convert a byte array with length = 2 * Integer.BYTES - 1 to an int array
        Assert.assertThrows(AssertionError.class, () -> IntUtils.byteArrayToIntArray(new byte[2 * Integer.BYTES - 1]));
        // convert a byte array with length = 2 * Integer.BYTES + 1 to an int array
        Assert.assertThrows(AssertionError.class, () -> IntUtils.byteArrayToIntArray(new byte[2 * Integer.BYTES + 1]));
    }

    @Test
    public void testIntArrayByteArray() {
        testIntArrayByteArray(new int[]{0x00}, new byte[]{0x00, 0x00, 0x00, 0x00,});
        testIntArrayByteArray(
            new int[]{Integer.MIN_VALUE, 0, -1, 1, Integer.MAX_VALUE},
            new byte[]{
                (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
                (byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            }
        );
    }

    private void testIntArrayByteArray(int[] intArray, byte[] byteArray) {
        byte[] convertByteArray = IntUtils.intArrayToByteArray(intArray);
        Assert.assertArrayEquals(byteArray, convertByteArray);
        int[] convertIntArray = IntUtils.byteArrayToIntArray(byteArray);
        Assert.assertArrayEquals(intArray, convertIntArray);
    }

    @Test
    public void testGetLittleEndianBoolean() {
        Assert.assertFalse(IntUtils.getLittleEndianBoolean(0b10000001_01111111_01001011_11010010, 0));
        Assert.assertTrue(IntUtils.getLittleEndianBoolean(0b10000001_01111111_01001011_11010010, 1));
        Assert.assertFalse(IntUtils.getLittleEndianBoolean(0b10000001_01111111_01001011_11010010, 2));
        Assert.assertFalse(IntUtils.getLittleEndianBoolean(0b10000001_01111111_01001011_11010010, 3));
        Assert.assertTrue(IntUtils.getLittleEndianBoolean(0b10000001_01111111_01001011_11010010, 4));
        Assert.assertFalse(IntUtils.getLittleEndianBoolean(0b10000001_01111111_01001011_11010010, 5));
        Assert.assertTrue(IntUtils.getLittleEndianBoolean(0b10000001_01111111_01001011_11010010, 6));
        Assert.assertTrue(IntUtils.getLittleEndianBoolean(0b10000001_01111111_01001011_11010010, 7));

        Assert.assertTrue(IntUtils.getLittleEndianBoolean(0b10000001_01111111_01001011_11010010, 31));
        Assert.assertFalse(IntUtils.getLittleEndianBoolean(0b10000001_01111111_01001011_11010010, 30));
        Assert.assertFalse(IntUtils.getLittleEndianBoolean(0b10000001_01111111_01001011_11010010, 29));
        Assert.assertFalse(IntUtils.getLittleEndianBoolean(0b10000001_01111111_01001011_11010010, 28));
        Assert.assertFalse(IntUtils.getLittleEndianBoolean(0b10000001_01111111_01001011_11010010, 27));
        Assert.assertFalse(IntUtils.getLittleEndianBoolean(0b10000001_01111111_01001011_11010010, 26));
        Assert.assertFalse(IntUtils.getLittleEndianBoolean(0b10000001_01111111_01001011_11010010, 25));
        Assert.assertTrue(IntUtils.getLittleEndianBoolean(0b10000001_01111111_01001011_11010010, 24));
    }
}

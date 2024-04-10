package edu.alibaba.mpc4j.common.tool.utils;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * 布尔工具类测试。
 *
 * @author Weiran Liu
 * @date 2021/11/29
 */
public class BinaryUtilsTest {
    /**
     * 最大随机测试轮数
     */
    private static final int MAX_RANDOM_ROUND = 400;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testInvalidBinaryByte() {
        // convert binary with length 0 to byte
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToByte(new boolean[0]));
        // convert binary with less length to byte
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToByte(new boolean[1]));
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToByte(new boolean[Byte.SIZE - 1]));
        // convert binary with long length to byte
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToByte(new boolean[Byte.SIZE + 1]));
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToByte(new boolean[Byte.SIZE * 2]));
    }

    @Test
    public void testBinaryByte() {
        testBinaryByte(new boolean[] {false, false, false, false, false, false, false, false, }, (byte)0b00000000);
        testBinaryByte(new boolean[] {false, false, false, false, false, false, false, true, }, (byte)0b00000001);
        testBinaryByte(new boolean[] {false, false, false, false, false, false, true, false, }, (byte)0b00000010);
        testBinaryByte(new boolean[] {false, false, false, false, false, true, false, false, }, (byte)0b00000100);
        testBinaryByte(new boolean[] {false, false, false, false, true, false, false, false, }, (byte)0b00001000);
        testBinaryByte(new boolean[] {false, false, false, true, false, false, false, false, }, (byte)0b00010000);
        testBinaryByte(new boolean[] {false, false, true, false, false, false, false, false, }, (byte)0b00100000);
        testBinaryByte(new boolean[] {false, true, false, false, false, false, false, false, }, (byte)0b01000000);
        testBinaryByte(new boolean[] {true, false, false, false, false, false, false, false, }, (byte)0b10000000);
    }

    private void testBinaryByte(boolean[] binary, byte byteValue) {
        byte convertByteValue = BinaryUtils.binaryToByte(binary);
        Assert.assertEquals(byteValue, convertByteValue);
        boolean[] convertBinary = BinaryUtils.byteToBinary(convertByteValue);
        Assert.assertArrayEquals(binary, convertBinary);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testInvalidBinaryLong() {
        // convert binary with length 0 to long
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToLong(new boolean[0]));
        // convert binary with less length to long
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToLong(new boolean[1]));
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToLong(new boolean[Long.SIZE - 1]));
        // convert binary with long length to long
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToLong(new boolean[Long.SIZE + 1]));
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToLong(new boolean[Long.SIZE * 2]));
    }

    @Test
    public void testBinaryLong() {
        // 0的转换
        boolean[] binary = new boolean[Long.SIZE];
        long longValue = 0;
        testBinaryLong(binary, longValue);
        // 从前到后转换
        binary[0] = true;
        longValue = 0x8000000000000000L;
        testBinaryLong(binary, longValue);
        for (int index = 1; index < Long.SIZE; index++) {
            binary[index - 1] = false;
            binary[index] = true;
            // 这里要使用无符号移位操作
            longValue = longValue >>> 1;
            testBinaryLong(binary, longValue);
        }
    }

    private void testBinaryLong(boolean[] binary, long longValue) {
        long convertLongValue = BinaryUtils.binaryToLong(binary);
        Assert.assertEquals(longValue, convertLongValue);
        boolean[] convertBinary = BinaryUtils.longToBinary(longValue);
        Assert.assertArrayEquals(binary, convertBinary);
    }

    @Test
    public void testInvalidBinaryToByteArray() {
        // convert binary with less length to byte array
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToByteArray(new boolean[1]));
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToByteArray(new boolean[Byte.SIZE - 1]));
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToByteArray(new boolean[Byte.SIZE * 2 - 1]));
        // convert binary with long length to byte array
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToByteArray(new boolean[Byte.SIZE + 1]));
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToByteArray(new boolean[Byte.SIZE * 2 + 1]));
    }

    @Test
    public void testConstantBinaryByteArray() {
        testConstantBinaryToByteArray(
            new boolean[] {false, true, false, false, false, false, true, true,},
            new byte[] {0b01000011,}
        );
        testConstantBinaryToByteArray(
            new boolean[] {
                false, false, true, true, true, false, true, false,
                false, true, false, false, false, false, true, true,
            },
            new byte[] {0b00111010, 0b01000011,}
        );
    }

    private void testConstantBinaryToByteArray(boolean[] binary, byte[] byteArray) {
        byte[] convertByteArray = BinaryUtils.binaryToByteArray(binary);
        Assert.assertArrayEquals(byteArray, convertByteArray);
        boolean[] convertBinary = BinaryUtils.byteArrayToBinary(byteArray);
        Assert.assertArrayEquals(binary, convertBinary);
    }

    @Test
    public void testRandomBinaryToByteArray() {
        testRandomBinaryToByteArray(0);
        testRandomBinaryToByteArray(1);
        testRandomBinaryToByteArray(2);
        testRandomBinaryToByteArray(8);
        testRandomBinaryToByteArray(CommonConstants.STATS_BYTE_LENGTH - 1);
        testRandomBinaryToByteArray(CommonConstants.STATS_BYTE_LENGTH);
        testRandomBinaryToByteArray(CommonConstants.STATS_BYTE_LENGTH + 1);
        testRandomBinaryToByteArray(CommonConstants.BLOCK_BYTE_LENGTH - 1);
        testRandomBinaryToByteArray(CommonConstants.BLOCK_BYTE_LENGTH);
        testRandomBinaryToByteArray(CommonConstants.BLOCK_BYTE_LENGTH + 1);
    }

    private void testRandomBinaryToByteArray(int byteLength) {
        int binaryLength = byteLength * Byte.SIZE;
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            boolean[] binary = new boolean[binaryLength];
            IntStream.range(0, binaryLength).forEach(index -> binary[index] = SECURE_RANDOM.nextBoolean());
            byte[] convertByteArray = BinaryUtils.binaryToByteArray(binary);
            boolean[] convertBinary = BinaryUtils.byteArrayToBinary(convertByteArray);
            Assert.assertArrayEquals(binary, convertBinary);
        }
    }

    @Test
    public void testInvalidBinaryToLongArray() {
        // convert binary with less length to long array
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToLongArray(new boolean[1]));
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToLongArray(new boolean[Long.SIZE - 1]));
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToLongArray(new boolean[Long.SIZE * 2 - 1]));
        // convert binary with long length to long array
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToLongArray(new boolean[Long.SIZE + 1]));
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.binaryToLongArray(new boolean[Long.SIZE * 2 + 1]));
    }

    @Test
    public void testRandomBinaryToLongArray() {
        testRandomBinaryToLongArray(0);
        testRandomBinaryToLongArray(1);
        testRandomBinaryToLongArray(2);
        testRandomBinaryToLongArray(8);
        testRandomBinaryToLongArray(CommonConstants.STATS_BYTE_LENGTH - 1);
        testRandomBinaryToLongArray(CommonConstants.STATS_BYTE_LENGTH);
        testRandomBinaryToLongArray(CommonConstants.STATS_BYTE_LENGTH + 1);
        testRandomBinaryToLongArray(CommonConstants.BLOCK_BYTE_LENGTH - 1);
        testRandomBinaryToLongArray(CommonConstants.BLOCK_BYTE_LENGTH);
        testRandomBinaryToLongArray(CommonConstants.BLOCK_BYTE_LENGTH + 1);
    }

    private void testRandomBinaryToLongArray(int longLength) {
        int binaryLength = longLength * Long.SIZE;
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            boolean[] binary = new boolean[binaryLength];
            IntStream.range(0, binaryLength).forEach(index -> binary[index] = SECURE_RANDOM.nextBoolean());
            long[] convertLongArray = BinaryUtils.binaryToLongArray(binary);
            boolean[] convertBinary = BinaryUtils.longArrayToBinary(convertLongArray);
            Assert.assertArrayEquals(binary, convertBinary);
        }
    }

    @Test
    public void testInvalidByteArrayToBinary() {
        // convert byte[] to binary with long length
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.byteArrayToBinary(new byte[1], Byte.SIZE + 1));
        Assert.assertThrows(AssertionError.class, () -> BinaryUtils.byteArrayToBinary(new byte[1], Byte.SIZE * 2));
    }

    @Test
    public void testConstantByteArrayToBinary() {
        // 0x03 <-> 11
        testConstantByteArrayBinary(
            new byte[] {0b00000011, }, new boolean[] {true, true, }
        );
        // 0x03 <-> 11
        testConstantByteArrayBinary(
            new byte[] {0b00000011, }, new boolean[] {false, true, true, }
        );
        // 0x03 <-> 0011
        testConstantByteArrayBinary(
            new byte[] {0b00000011, }, new boolean[] {false, false, true, true, }
        );
        // 0x1F <-> 11111
        testConstantByteArrayBinary(
            new byte[] {0b00011111, }, new boolean[] {true, true, true, true, true, }
        );
        // 0x1F <-> 011111
        testConstantByteArrayBinary(
            new byte[] {0b00011111, }, new boolean[] {false, true, true, true, true, true, }
        );
        // 0x1F <-> 0011111
        testConstantByteArrayBinary(
            new byte[] {0b00011111, }, new boolean[] {false, false, true, true, true, true, true, }
        );
        // 0x1F <-> 00011111
        testConstantByteArrayBinary(
            new byte[] {0b00011111, }, new boolean[] {false, false, false, true, true, true, true, true, }
        );
    }

    private void testConstantByteArrayBinary(byte[] byteArray, boolean[] binary) {
        boolean[] convertBinary = BinaryUtils.byteArrayToBinary(byteArray, binary.length);
        Assert.assertArrayEquals(binary, convertBinary);
    }

    @Test
    public void testRandomLongArrayToBinary() {
        testRandomLongArrayToBinary(0);
        testRandomLongArrayToBinary(1);
        testRandomLongArrayToBinary(2);
        testRandomLongArrayToBinary(8);
        testRandomLongArrayToBinary(CommonConstants.STATS_BYTE_LENGTH - 1);
        testRandomLongArrayToBinary(CommonConstants.STATS_BYTE_LENGTH);
        testRandomLongArrayToBinary(CommonConstants.STATS_BYTE_LENGTH + 1);
        testRandomLongArrayToBinary(CommonConstants.BLOCK_BYTE_LENGTH - 1);
        testRandomLongArrayToBinary(CommonConstants.BLOCK_BYTE_LENGTH);
        testRandomLongArrayToBinary(CommonConstants.BLOCK_BYTE_LENGTH + 1);
    }

    private void testRandomLongArrayToBinary(int longLength) {
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            long[] longArray = LongStream.range(0, longLength).map(index -> SECURE_RANDOM.nextLong()).toArray();
            boolean[] convertBinary = BinaryUtils.longArrayToBinary(longArray);
            long[] convertLongArray = BinaryUtils.binaryToLongArray(convertBinary);
            Assert.assertArrayEquals(longArray, convertLongArray);
        }
    }

    @Test
    public void testByteArrayGetBoolean() {
        testByteArrayGetBoolean(
            // 0x43 <-> 01000011
            new byte[] {0b01000011, },
            new boolean[] {false, true, false, false, false, false, true, true,
            }
        );
        testByteArrayGetBoolean(
            // 0x3A,0x43 <-> 00111010,01000011
            new byte[] {0b00111010, 0b01000011, },
            new boolean[] {
                false, false, true, true, true, false, true, false,
                false, true, false, false, false, false, true, true,
            }
        );
    }

    private void testByteArrayGetBoolean(byte[] byteArray, boolean[] binary) {
        IntStream.range(0, binary.length).forEach(binaryIndex ->
            Assert.assertEquals(binary[binaryIndex], BinaryUtils.getBoolean(byteArray, binaryIndex))
        );
    }

    @Test
    public void testLongArrayGetBoolean() {
        testLongArrayGetBoolean(
            // 0x43,00,00,00,00,00,00,43L
            new long[] {0b01000011_00000000_00000000_00000000_00000000_00000000_00000000_01000011L, },
            // 01000011,00000000,00000000,00000000,00000000,00000000,00000000,01000011
            new boolean[] {
                false, true, false, false, false, false, true, true,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, true, false, false, false, false, true, true,
            }
        );
        testLongArrayGetBoolean(
            // 0x3A,43,00,00,00,00,00,00L, 0x00,00,00,00,00,00,3A,43L
            new long[] {
                0b00111010_01000011_00000000_00000000_00000000_00000000_00000000_00000000L,
                0b00000000_00000000_00000000_00000000_00000000_00000000_00111010_01000011L, },
            // 00111010,01000011,00000000,00000000,00000000,00000000,00000000,00000000
            // 00000000,00000000,00000000,00000000,00000000,00000000,00111010,01000011
            new boolean[] {
                false, false, true, true, true, false, true, false,
                false, true, false, false, false, false, true, true,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false,
                false, false, true, true, true, false, true, false,
                false, true, false, false, false, false, true, true,
            }
        );
    }

    private void testLongArrayGetBoolean(long[] longArray, boolean[] binary) {
        IntStream.range(0, binary.length).forEach(binaryIndex ->
            Assert.assertEquals(binary[binaryIndex], BinaryUtils.getBoolean(longArray, binaryIndex))
        );
    }

    @Test
    public void testByteArraySetBoolean() {
        testByteArraySetBoolean(new byte[] {0b01000011, });
        testByteArraySetBoolean(new byte[] {0b00111010, 0b01000011, });
    }

    private void testByteArraySetBoolean(byte[] byteArray) {
        // 测试每一位置设置为1
        byte[] trueByteArray = BytesUtils.clone(byteArray);
        IntStream.range(0, trueByteArray.length * Byte.SIZE).forEach(binaryIndex -> {
            BinaryUtils.setBoolean(trueByteArray, binaryIndex, true);
            Assert.assertTrue(BinaryUtils.getBoolean(trueByteArray, binaryIndex));
        });
        // 测试每一位置设置为0
        byte[] falseByteArray = BytesUtils.clone(byteArray);
        IntStream.range(0, trueByteArray.length * Byte.SIZE).forEach(binaryIndex -> {
            BinaryUtils.setBoolean(falseByteArray, binaryIndex, false);
            Assert.assertFalse(BinaryUtils.getBoolean(falseByteArray, binaryIndex));
        });
    }

    @Test
    public void testLongArraySetBoolean() {
        testLongArraySetBoolean(new long[] {0x4300000000000043L, });
        testLongArraySetBoolean(new long[] {0x3A43000000000000L, 0x0000000000003A43L, });
    }

    private void testLongArraySetBoolean(long[] longArray) {
        // 测试每一位置设置为1
        long[] trueLongArray = LongUtils.clone(longArray);
        IntStream.range(0, trueLongArray.length * Long.SIZE).forEach(binaryIndex -> {
            BinaryUtils.setBoolean(trueLongArray, binaryIndex, true);
            Assert.assertTrue(BinaryUtils.getBoolean(trueLongArray, binaryIndex));
        });
        // 测试每一位置设置为0
        long[] falseByteArray = LongUtils.clone(longArray);
        IntStream.range(0, trueLongArray.length * Long.SIZE).forEach(binaryIndex -> {
            BinaryUtils.setBoolean(falseByteArray, binaryIndex, false);
            Assert.assertFalse(BinaryUtils.getBoolean(falseByteArray, binaryIndex));
        });
    }

    @Test
    public void testUncheckByteArrayToBinary(){
        for(int i = 0; i < 10; i++){
            byte[] data = new byte[SECURE_RANDOM.nextInt(128) + 1];
            SECURE_RANDOM.nextBytes(data);
            boolean[] all = BinaryUtils.byteArrayToBinary(data);
            int keepBit = SECURE_RANDOM.nextInt(data.length << 3);
            boolean[] res = BinaryUtils.uncheckByteArrayToBinary(data, keepBit);
            Assert.assertArrayEquals(res, Arrays.copyOfRange(all, all.length - keepBit, all.length));
        }
    }
}

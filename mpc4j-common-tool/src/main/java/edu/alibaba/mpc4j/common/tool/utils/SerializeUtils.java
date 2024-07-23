package edu.alibaba.mpc4j.common.tool.utils;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory.BitVectorType;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * serialize utilities.
 *
 * @author Weiran Liu
 * @date 2024/1/3
 */
public class SerializeUtils {

    /**
     * private constructor.
     */
    private SerializeUtils() {
        // empty
    }

    /**
     * Compresses unequal-size data.
     *
     * @param origin original data.
     * @return compressed data.
     */
    public static byte[] compressUnequal(List<byte[]> origin) {
        long longSize = origin.stream().mapToLong(row -> row.length + Integer.BYTES).sum() + Integer.BYTES;
        MathPreconditions.checkLessOrEqual("size", longSize, Integer.MAX_VALUE);
        int size = (int) longSize;
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        // add size
        byteBuffer.putInt(origin.size());
        for (byte[] data : origin) {
            // write length
            byteBuffer.putInt(data.length);
            // write data
            byteBuffer.put(data);
        }
        return byteBuffer.array();
    }

    /**
     * Decompresses unequal-size data.
     *
     * @param compressed compressed data.
     * @return original data.
     */
    public static List<byte[]> decompressUnequal(byte[] compressed) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(compressed);
        int size = byteBuffer.getInt();
        byte[][] rowData = new byte[size][];
        int index = 0;
        while (byteBuffer.position() < compressed.length) {
            int rowSize = byteBuffer.getInt();
            rowData[index] = new byte[rowSize];
            byteBuffer.get(rowData[index]);
            index++;
        }
        return Arrays.stream(rowData).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Compresses equal-size data.
     *
     * @param origin original data.
     * @param length length for each data.
     * @return compressed data.
     */
    public static byte[] compressEqual(List<byte[]> origin, int length) {
        // we allow equal-size with length = 0
        MathPreconditions.checkNonNegative("length", length);
        if (length == 0) {
            return IntUtils.intToByteArray(origin.size());
        }
        long longSize = (long) length * origin.size();
        MathPreconditions.checkLessOrEqual("size", longSize, Integer.MAX_VALUE);
        int size = (int) longSize;
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        for (byte[] data : origin) {
            MathPreconditions.checkEqual("expect", "actual", length, data.length);
            // write data
            byteBuffer.put(data);
        }
        return byteBuffer.array();
    }

    /**
     * Decompresses equal-size data.
     *
     * @param compressed compressed data.
     * @param length     length for each data.
     * @return original data.
     */
    public static List<byte[]> decompressEqual(byte[] compressed, int length) {
        MathPreconditions.checkNonNegative("length", length);
        if (length == 0) {
            int size = IntUtils.byteArrayToInt(compressed);
            return IntStream.range(0, size).mapToObj(i -> new byte[0]).collect(Collectors.toCollection(ArrayList::new));
        }
        int size = compressed.length / length;
        byte[][] rowData = new byte[size][length];
        int rowIndex = 0;
        // read data
        ByteBuffer byteBuffer = ByteBuffer.wrap(compressed);
        while (byteBuffer.position() < compressed.length) {
            byteBuffer.get(rowData[rowIndex]);
            rowIndex++;
        }
        return Arrays.stream(rowData).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Compresses equal-size data, where the bit length for each data is 1.
     *
     * @param origin original data.
     * @return compressed data.
     */
    public static byte[] compressL1(byte[] origin) {
        int bitNum = origin.length;
        BitVector bitVector = BitVectorFactory.createZeros(BitVectorType.BYTES_BIT_VECTOR, bitNum);
        int index = 0;
        for (byte data : origin) {
            assert ((data & 0b00000001) == data);
            bitVector.set(index, data == 1);
            index++;
        }
        return bitVector.getBytes();
    }

    /**
     * Decompress equal-size data, where the bit length for each data is 1.
     *
     * @param compressed compressed data.
     * @param size       size of the original data.
     * @return original data.
     */
    public static byte[] decompressL1(byte[] compressed, int size) {
        BitVector bitVector = BitVectorFactory.create(BitVectorType.BYTES_BIT_VECTOR, size, compressed);
        byte[] origin = new byte[size];
        for (int index = 0; index < size; index++) {
            origin[index] = bitVector.get(index) ? (byte) 0b00000001 : (byte) 0b00000000;
        }
        return origin;
    }

    /**
     * Compresses equal-size data, where the bit length for each data is 2.
     *
     * @param origin original data.
     * @return compressed data.
     */
    public static byte[] compressL2(byte[] origin) {
        int size = origin.length;
        byte[] compressed = new byte[CommonUtils.getUnitNum(size, 4)];
        int index = 0;
        for (byte data : origin) {
            assert (data & 0b00000011) == data;
            compressed[(size - 1 - index) / 4] |= (byte) (data << (index * 2) % 8);
            index++;
        }
        return compressed;
    }

    /**
     * Decompress equal-size data, where the bit length for each data is 2.
     *
     * @param compressed compressed data.
     * @param size       size of the original data.
     * @return original data.
     */
    public static byte[] decompressL2(byte[] compressed, int size) {
        assert compressed.length == CommonUtils.getUnitNum(size, 4);
        byte[] origin = new byte[size];
        for (int index = 0; index < size; index++) {
            origin[index] = (byte) ((compressed[(size - 1 - index) / 4] >>> ((index * 2) % 8)) & 0b00000011);
        }
        return origin;
    }

    /**
     * Compresses equal-size data, where the bit length for each data is 4.
     *
     * @param origin original data.
     * @return compressed data.
     */
    public static byte[] compressL4(byte[] origin) {
        int size = origin.length;
        byte[] compressed = new byte[CommonUtils.getUnitNum(size, 2)];
        int index = 0;
        for (byte data : origin) {
            assert ((data & 0b00001111) == data);
            compressed[(size - 1 - index) / 2] |= (byte) (data << (index * 4) % 8);
            index++;
        }
        return compressed;
    }

    /**
     * Decompress equal-size data, where the bit length for each data is 4.
     *
     * @param compressed compressed data.
     * @param size       size of the original data.
     * @return original data.
     */
    public static byte[] decompressL4(byte[] compressed, int size) {
        assert compressed.length == CommonUtils.getUnitNum(size, 2);
        byte[] origin = new byte[size];
        for (int index = 0; index < size; index++) {
            origin[index] = (byte) ((compressed[(size - 1 - index) / 2] >>> ((index * 4) % 8)) & 0b00001111);
        }
        return origin;
    }
}

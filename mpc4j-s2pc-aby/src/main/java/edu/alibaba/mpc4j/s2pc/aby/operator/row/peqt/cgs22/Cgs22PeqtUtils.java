package edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.cgs22;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Utilities used in CSG22 private equality test.
 *
 * @author Li Peng
 * @date 2024/6/5
 */
public class Cgs22PeqtUtils {
    /**
     * Mask table.
     */
    public static final byte[] MASK = new byte[]{
        (byte) 0x01, (byte) 0x03, (byte) 0x07, (byte) 0x0F,
        (byte) 0x1F, (byte) 0x3F, (byte) 0x7F, (byte) 0xFF,
    };

    /**
     * Unsigned/logical right shift of whole byte array by shiftBitCount bits.
     * This method will alter the input byte array.
     * See https://stackoverflow.com/questions/28997781/bit-shift-operations-on-a-byte-array-in-java.
     *
     * @param byteArray     input byte array.
     * @param shiftBitCount count of shift bits.
     */
    public static void shiftRight(byte[] byteArray, int shiftBitCount) {
        final int shiftMod = shiftBitCount % 8;
        final byte carryMask = (byte) (0xFF << (8 - shiftMod));
        final int offsetBytes = (shiftBitCount / 8);

        int sourceIndex;
        for (int i = byteArray.length - 1; i >= 0; i--) {
            sourceIndex = i - offsetBytes;
            if (sourceIndex < 0) {
                byteArray[i] = 0;
            } else {
                byte src = byteArray[sourceIndex];
                byte dst = (byte) ((0xff & src) >>> shiftMod);
                if (sourceIndex - 1 >= 0) {
                    dst |= byteArray[sourceIndex - 1] << (8 - shiftMod) & carryMask;
                }
                byteArray[i] = dst;
            }
        }
    }

    /**
     * Partition inputs into blocks.
     *
     * @param inputs inputs.
     * @param m      block size.
     * @param q      number of blocks.
     * @return blocks of inputs.
     */
    public static int[][] partitionInputArray(byte[][] inputs, int m, int q) {
        // P1 parses each of its input element as y_{q-1} || ... || y_{0}, where y_j ∈ {0,1}^m for all j ∈ [0,q).
        int num = inputs.length;
        int[][] partitionInputArray = new int[q][num];
        IntStream.range(0, num).forEach(index -> {
            byte[] y = Arrays.copyOf(inputs[index], inputs[index].length);
            for (int lIndex = 0; lIndex < q; lIndex++) {
                int lIndexByte = y[y.length - 1] & MASK[m - 1];
                partitionInputArray[lIndex][index] = lIndexByte;
                shiftRight(y, m);
            }
        });
        return partitionInputArray;
    }
}

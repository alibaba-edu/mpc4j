package edu.alibaba.mpc4j.s2pc.aby.edit;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Edit distance utilities.
 *
 * @author Feng Han, Li Peng
 * @date 2024/4/9
 */
public class EditUtils {

    /**
     * Get bytes of all chars in current batch. Order is arranged from left to right, and from top to bottom.
     *
     * @param data          strings.
     * @param otherLen      lengths of strings from the other party.
     * @param totalNum      number of chars in current batch.
     * @param strStartIndex start index of strings.
     * @param strEndIndex   end index of strings.
     * @param isReceiver    the party is receiver.
     * @return bytes of all chars.
     */
    public static byte[][] getSingleBatchBytesSimple(String[] data, int[] otherLen, int totalNum,
                                                     int strStartIndex, int strEndIndex, boolean isReceiver) {
        byte[][] res = new byte[totalNum][];
        int startIndex = 0;
        if (isReceiver) {
            for (int cIndex = strStartIndex; cIndex < strEndIndex; cIndex++) {
                byte[] sBytes = data[cIndex].toLowerCase().replace(" ", " ").getBytes();
                // receiver：aaabbbcccddd
                for (byte sByte : sBytes) {
                    for (int j = 0; j < otherLen[cIndex]; j++) {
                        res[startIndex++] = new byte[]{sByte};
                    }
                }
            }

        } else {
            for (int cIndex = strStartIndex; cIndex < strEndIndex; cIndex++) {
                byte[] sBytes = data[cIndex].toLowerCase().replace(" ", " ").getBytes();
                // sender:efgefgefgefg
                for (int i = 0; i < otherLen[cIndex]; i++) {
                    for (byte sByte : sBytes) {
                        res[startIndex++] = new byte[]{sByte};
                    }
                }
            }
        }
        return res;
    }

    /**
     * Get the index and number of chars.
     *
     * @param data         strings.
     * @param otherLen     lengths of strings from the other party.
     * @param maxBatchSize max batch size
     * @return the start index of the first char in next batch, and the number of chars in current batch.
     */
    public static int[][] getSepIndexAndNum(String[] data, int[] otherLen, int maxBatchSize) {
        MathPreconditions.checkEqual("data.length", "otherLen.length", data.length, otherLen.length);
        MathPreconditions.checkPositive("maxBatchSize", maxBatchSize);
        TIntList indexList = new TIntLinkedList();
        TIntList numList = new TIntLinkedList();

        // split data and find boundary.
        int currentSum = data[0].length() * otherLen[0];
        for (int i = 1; i < data.length; i++) {
            int addNum = data[i].length() * otherLen[i];
            if (currentSum + addNum > maxBatchSize) {
                // store the index of last str in current batch.
                indexList.add(i);
                // store the total number of chars in current batch.
                numList.add(currentSum);
                currentSum = 0;
            }
            currentSum += addNum;
        }
        if (currentSum <= maxBatchSize) {
            indexList.add(data.length);
            numList.add(currentSum);
        }
        return new int[][]{indexList.toArray(), numList.toArray()};
    }

    /**
     * Get the (inner) coordinates in current compute time (in diagonal computing).
     * The range of y (rows) is [0, rowNum - 1], the range of x (columns) is [0, columnNum - 1].
     *
     * @param computeTime compute time, from 0.
     * @param rowNum      number of rows.
     * @param columnNum   number of columns.
     * @param needPrune   need to prune unneeded cells.
     * @return the coordinates in current compute time, represented in two arrays (y,x).
     */
    public static int[][] getCoordi(int computeTime, int rowNum, int columnNum, boolean needPrune) {
        assert computeTime >= 0;
        assert rowNum >= 0;
        assert columnNum >= 0;
        if (computeTime >= rowNum * columnNum) {
            return new int[2][];
        }
        int[] y = new int[0], x = new int[0];
        if (computeTime < rowNum & computeTime < columnNum) {
            y = IntStream.range(0, computeTime + 1).boxed().sorted(Collections.reverseOrder()).mapToInt(i -> i).toArray();
            x = IntStream.range(0, computeTime + 1).toArray();
        }
        // vertical rectangle
        if (computeTime < rowNum & computeTime >= columnNum) {
            y = IntStream.range(computeTime - columnNum + 1, computeTime + 1).boxed().sorted(Collections.reverseOrder()).mapToInt(i -> i).toArray();

            x = IntStream.range(0, columnNum).toArray();
        }
        // horizontal rectangle
        if (computeTime >= rowNum & computeTime < columnNum) {
            y = IntStream.range(0, rowNum).boxed().sorted(Collections.reverseOrder()).mapToInt(i -> i).toArray();
            x = IntStream.range(computeTime - rowNum + 1, computeTime + 1).toArray();
        }
        if (computeTime >= rowNum & computeTime >= columnNum) {
            y = IntStream.range(computeTime - columnNum + 1, rowNum).boxed().sorted(Collections.reverseOrder()).mapToInt(i -> i).toArray();
            x = IntStream.range(computeTime - rowNum + 1, columnNum).toArray();
        }
        assert x.length == y.length;
        if (needPrune) {
            List<Integer> yList = new ArrayList<>();
            List<Integer> xList = new ArrayList<>();
            for (int i = 0; i < x.length; i++) {
                if (!isPrunedIndex(y[i] + 1, x[i] + 1, rowNum + 1, columnNum + 1)) {
                    yList.add(y[i]);
                    xList.add(x[i]);
                }
            }
            y = yList.stream().mapToInt(i -> i).toArray();
            x = xList.stream().mapToInt(i -> i).toArray();
        }
        return new int[][]{y, x};
    }

    public static boolean isPrunedIndex(int y, int x, int rowNum, int columnNum) {
        int maxNum = Math.max(rowNum, columnNum);
        return Math.abs(2 * (y - x) + columnNum - rowNum) > maxNum;
    }

    /**
     * Get the offset from coordinates in matrix.
     *
     * @param columnNum number of columns.
     * @param y         y coordinate.
     * @param x         x coordinate.
     * @return offset.
     */
    public static int getOffsetFromCoordi(int columnNum, int y, int x) {
        return y * columnNum + x;
    }

    /**
     * Get required bit length from a value.
     *
     * @param maxValue value.
     * @return required bit length
     */
    public static int getBitRequired(int maxValue) {
        if (maxValue < 0) {
            return 0;
        }
        return LongUtils.ceilLog2(maxValue + 1);
    }

    /**
     * Update matrix based on values and locations.
     *
     * @param matrix the matrix.
     * @param values the values to be updated.
     * @param coordi coordinates.
     */
    public static void updateMatrix(BigInteger[][][] matrix, BigInteger[] values, int[][][] coordi) {
        int strNum = coordi.length;
        int index = 0;
        for (int strIndex = 0; strIndex < strNum; strIndex++) {
            int coordiNum = coordi[strIndex][0] == null ? 0 : coordi[strIndex][0].length;
            if (coordiNum == 0) {
                continue;
            }
            for (int coordiIndex = 0; coordiIndex < coordiNum; coordiIndex++) {
                matrix[strIndex][coordi[strIndex][0][coordiIndex] + 1][coordi[strIndex][1][coordiIndex] + 1] = values[index++];
            }
        }
    }
}

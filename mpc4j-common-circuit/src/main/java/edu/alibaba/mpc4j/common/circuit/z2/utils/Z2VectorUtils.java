package edu.alibaba.mpc4j.common.circuit.z2.utils;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Utilities of z2 vector
 *
 * @author Feng Han
 * @date 2023/10/30
 */
public class Z2VectorUtils {
    public static long[] transport(MpcZ2Vector[] data) {
        BitVector[] permutationVec = Arrays.stream(data).map(MpcZ2Vector::getBitVector).toArray(BitVector[]::new);
        BigInteger[] permutationVecTrans = ZlDatabase.create(EnvType.STANDARD, false, permutationVec).getBigIntegerData();
        return Arrays.stream(permutationVecTrans).mapToLong(BigInteger::longValue).toArray();
    }

    /**
     * get the mask for bitonic sorter
     *
     * @param log2 ceilLog of data number
     * @return the mask value for 1<<(log2-1) comparison
     */
    public static byte[][] returnCompareResultMask(int log2) {
        byte[][] compareResultMask = new byte[log2 - 1][];
        // the first three masks are 01010101..., 00110011..., 00001111...
        int byteNum = log2 < 4 ? 1 : 1 << (log2 - 4);
        IntStream.range(0, log2 - 1).parallel().forEach(i -> {
            byte[] tmpByte = new byte[byteNum];
            if (i == 0) {
                Arrays.fill(tmpByte, (byte) 0b01010101);
            } else if (i == 1) {
                Arrays.fill(tmpByte, (byte) 0b00110011);
            } else if (i == 2) {
                Arrays.fill(tmpByte, (byte) 0b00001111);
            } else {
                int interval = 1 << (i - 3);
                int groupNum = 1 << (log2 - 2 - i);
                IntStream.range(0, groupNum).forEach(j ->
                    Arrays.fill(tmpByte, (2 * j + 1) * interval, (2 * j + 2) * interval, (byte) 255));
            }
            compareResultMask[i] = tmpByte;
        });
        return compareResultMask;
    }

    /**
     * get the index for data [0, length - 1].
     *
     * @param length the length of index.
     * @return the binary value of indexes in column form.
     */
    public static BitVector[] getBinaryIndex(int length) {
        byte[][] compareResultMask = returnCompareResultMask(LongUtils.ceilLog2(length) + 1);
        int bitShift = (1 << LongUtils.ceilLog2(length)) - length;
        if (bitShift == 0) {
            if (length < 8) {
                return IntStream.range(0, compareResultMask.length).mapToObj(i -> {
                    BytesUtils.reduceByteArray(compareResultMask[compareResultMask.length - 1 - i], length);
                    return BitVectorFactory.create(length, compareResultMask[compareResultMask.length - 1 - i]);
                }).toArray(BitVector[]::new);
            } else {
                return IntStream.range(0, compareResultMask.length).mapToObj(i ->
                    BitVectorFactory.create(length, compareResultMask[compareResultMask.length - 1 - i])).toArray(BitVector[]::new);
            }
        } else {
            return IntStream.range(0, compareResultMask.length).mapToObj(i ->
                    BitVectorFactory.create(length, BytesUtils.createReduceByteArray(
                        BytesUtils.shiftRight(compareResultMask[compareResultMask.length - 1 - i], bitShift), length)))
                .toArray(BitVector[]::new);
        }
    }

    /**
     * extend the bits of specific positions with fixed skip length from the end to the front.
     * if destBitLen % skipLen > 0, then there are 0s in the first group.
     * For example, given data = abc, skipLen = 2 and destBitLen = 5
     * the return vectors are [abcbc]
     * given data = abcde, skipLen = 4 and totalBitNum = 13
     * the return vectors are [a000a,bcdebcde]
     *
     * @param data       source data
     * @param destBitLen target bit length
     * @param skipLen    skip bit length in source data
     */
    public static byte[] extendBitsWithSkip(BitVector data, int destBitLen, int skipLen) {
        MathPreconditions.checkEqual("skipLen", "2^k", 1 << LongUtils.ceilLog2(skipLen), skipLen);
        int destByteNum = CommonUtils.getByteLength(destBitLen);
        byte[] destByte = new byte[destByteNum];
        int notFullNum = destBitLen % (skipLen << 1) - skipLen > 0 ? 1 : 0;
        int groupNum = destBitLen / (skipLen << 1) + notFullNum;
        // if the first part is not full, deal with the first part
        if (notFullNum > 0) {
            // if the first part is not full, the data should be picked out from the index of 0
            int destOffset = (destByteNum << 3) - destBitLen;
            int firstLen = destBitLen % skipLen;
            for (int i = 0; i < firstLen; i++, destOffset++) {
                if (data.get(i)) {
                    BinaryUtils.setBoolean(destByte, destOffset, true);
                    BinaryUtils.setBoolean(destByte, destOffset + skipLen, true);
                }
            }
        }
        // deal with the other parts
        byte[] srcByte = data.getBytes();
        if (skipLen >= 8) {
            int eachByteNum = skipLen >> 3, eachPartNum = eachByteNum << 1;
            int srcEndIndex = srcByte.length, destEndIndex = destByteNum;
            for (int i = groupNum - 1; i >= notFullNum; i--, destEndIndex -= eachPartNum, srcEndIndex -= eachByteNum) {
                System.arraycopy(srcByte, srcEndIndex - eachByteNum, destByte, destEndIndex - eachByteNum, eachByteNum);
                System.arraycopy(srcByte, srcEndIndex - eachByteNum, destByte, destEndIndex - eachPartNum, eachByteNum);
            }
        } else {
            int andNum = (1 << skipLen) - 1;
            int[] destShiftLeftBit;
            if (skipLen == 4) {
                destShiftLeftBit = new int[]{0, 0};
            } else if (skipLen == 2) {
                destShiftLeftBit = new int[]{0, 4, 0, 4};
            } else {
                destShiftLeftBit = new int[]{0, 2, 4, 6, 0, 2, 4, 6};
            }
            int groupInEachSrcByte = Byte.SIZE / skipLen;
            int[] groupInByteNum = new int[]{groupInEachSrcByte >> 1, groupInEachSrcByte};
            int currentDestByteIndex = destByteNum - 1, currentSrcByteIndex = srcByte.length - 1;

            int fullByteNum = (groupNum - notFullNum) / groupInEachSrcByte * groupInEachSrcByte;
            for (int i = 0; i < fullByteNum; i += groupInEachSrcByte, currentSrcByteIndex--) {
                int j = 0, currentSrc = srcByte[currentSrcByteIndex];
                for (int splitTwoByte = 0; splitTwoByte < 2; splitTwoByte++) {
                    byte record = 0x00;
                    for (; j < groupInByteNum[splitTwoByte]; j++) {
                        int tmp = (currentSrc & andNum) << destShiftLeftBit[j];
                        currentSrc >>>= skipLen;
                        byte dupValue = (byte) (tmp ^ (tmp << skipLen));
                        record ^= dupValue;
                    }
                    destByte[currentDestByteIndex--] = record;
                }
            }
            // deal with the parts that can not fill one byte
            if (fullByteNum != groupNum - notFullNum) {
                int lastGroupNum = groupNum - notFullNum - fullByteNum;
                int j = 0, currentSrc = srcByte[currentSrcByteIndex];
                for (int splitTwoByte = 0; splitTwoByte < 2 && j < lastGroupNum; splitTwoByte++) {
                    byte record = 0x00;
                    for (; j < groupInByteNum[splitTwoByte] && j < lastGroupNum; j++) {
                        int tmp = (currentSrc & andNum) << destShiftLeftBit[j];
                        currentSrc >>>= skipLen;
                        byte dupValue = (byte) (tmp ^ (tmp << skipLen));
                        record ^= dupValue;
                    }
                    destByte[currentDestByteIndex--] ^= record;
                }
            }
        }
        return destByte;
    }

    /**
     * get the bits of specific positions with fixed skip length from the end to the front.
     * For example, given data = abcdefg, skipLen = 2 and totalBitNum = 3
     * the return vectors are [ade, cfg]
     * given data = a,bcdefghi, skipLen = 1 and totalBitNum = 4
     * the return vectors are [bdfh, cegi]
     *
     * @param data        source data
     * @param totalBitNum how many bits should be picked out
     * @param skipLen     skip bit length in source data
     */
    public static byte[][] getBitsWithSkip(BitVector data, int totalBitNum, int skipLen) {
        MathPreconditions.checkEqual("skipLen", "2^k", 1 << LongUtils.ceilLog2(skipLen), skipLen);
        byte[] srcByte = data.getBytes();
        int destByteNum = CommonUtils.getByteLength(totalBitNum);
        byte[][] destByte = new byte[2][destByteNum];
        int groupNum = totalBitNum / skipLen + (totalBitNum % skipLen > 0 ? 1 : 0);

        // if the first part is not full, deal with the first part
        if (totalBitNum % skipLen > 0) {
            int destOffset = (destByteNum << 3) - totalBitNum;
            int firstLen = totalBitNum % skipLen;
            for (int i = 0; i < firstLen; i++, destOffset++) {
                if (data.get(i)) {
                    BinaryUtils.setBoolean(destByte[0], destOffset, true);
                }
                if (data.get(i + skipLen)) {
                    BinaryUtils.setBoolean(destByte[1], destOffset, true);
                }
            }
        }
        // deal with the other parts
        int notFullNum = totalBitNum % skipLen > 0 ? 1 : 0;
        if (skipLen >= 8) {
            int eachByteNum = skipLen >> 3, eachPartNum = eachByteNum << 1;
            int srcEndIndex = srcByte.length, destEndIndex = destByteNum;
            for (int i = groupNum - 1; i >= notFullNum; i--, destEndIndex -= eachByteNum, srcEndIndex -= eachPartNum) {
                System.arraycopy(srcByte, srcEndIndex - eachByteNum, destByte[1], destEndIndex - eachByteNum, eachByteNum);
                System.arraycopy(srcByte, srcEndIndex - eachPartNum, destByte[0], destEndIndex - eachByteNum, eachByteNum);
            }
        } else {
            int andNum = (1 << skipLen) - 1;
            int[] destShiftLeftBit;
            if (skipLen == 4) {
                destShiftLeftBit = new int[]{0, 4};
            } else if (skipLen == 2) {
                destShiftLeftBit = new int[]{0, 2, 4, 6};
            } else {
                destShiftLeftBit = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
            }
            int groupInEachDestByte = Byte.SIZE / skipLen;
            int[] groupInByteNum = new int[]{groupInEachDestByte >> 1, groupInEachDestByte};
            int currentDestByteIndex = destByteNum - 1, currentSrcByteIndex = srcByte.length - 1;

            int fullByteNum = (groupNum - notFullNum) / groupInEachDestByte * groupInEachDestByte;
            for (int i = 0; i < fullByteNum; i += groupInEachDestByte) {
                int j = 0, record0 = 0x00, record1 = 0x00;
                for (int splitTwoByte = 0; splitTwoByte < 2; splitTwoByte++, currentSrcByteIndex--) {
                    int currentSrc = srcByte[currentSrcByteIndex] & 0xff;
                    for (; j < groupInByteNum[splitTwoByte]; j++) {
                        record1 ^= (currentSrc & andNum) << destShiftLeftBit[j];
                        currentSrc >>>= skipLen;
                        record0 ^= (currentSrc & andNum) << destShiftLeftBit[j];
                        currentSrc >>>= skipLen;
                    }
                }
                destByte[0][currentDestByteIndex] = (byte) record0;
                destByte[1][currentDestByteIndex--] = (byte) record1;
            }
            // deal with the parts that can not fill one byte
            if (fullByteNum != groupNum - notFullNum) {
                int lastGroupNum = groupNum - notFullNum - fullByteNum;
                int j = 0, record0 = 0x00, record1 = 0x00;
                for (int splitTwoByte = 0; splitTwoByte < 2 && j < lastGroupNum; splitTwoByte++, currentSrcByteIndex--) {
                    int currentSrc = srcByte[currentSrcByteIndex] & 0xff;
                    for (; j < groupInByteNum[splitTwoByte] && j < lastGroupNum; j++) {
                        record1 ^= (currentSrc & andNum) << destShiftLeftBit[j];
                        currentSrc >>>= skipLen;
                        record0 ^= (currentSrc & andNum) << destShiftLeftBit[j];
                        currentSrc >>>= skipLen;
                    }
                }
                destByte[0][currentDestByteIndex] ^= (byte) record0;
                destByte[1][currentDestByteIndex] ^= (byte) record1;
            }
        }
        return destByte;
    }
}

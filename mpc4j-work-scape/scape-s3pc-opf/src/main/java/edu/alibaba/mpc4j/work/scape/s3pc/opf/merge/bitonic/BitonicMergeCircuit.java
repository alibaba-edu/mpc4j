package edu.alibaba.mpc4j.work.scape.s3pc.opf.merge.bitonic;

import edu.alibaba.mpc4j.common.circuit.z2.AbstractZ2Circuit;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * bitonic merge circuit.
 *
 * @author Feng Han
 * @date 2025/2/21
 */
public class BitonicMergeCircuit extends AbstractZ2Circuit {
    /**
     * Z2 integer circuit.
     */
    protected final Z2IntegerCircuit circuit;
    /**
     * original length of the left input
     */
    private int originalLeftLen;
    /**
     * original length of the right input
     */
    private int originalRightLen;
    /**
     * fixed total input length, equals to the minimum number with the form 2^k that is greater than originalLeftLen + originalRightLen
     */
    private int fixedTotalLen;
    /**
     * processedInput
     */
    private MpcZ2Vector[] fixedInput;

    public BitonicMergeCircuit(Z2IntegerCircuit circuit) {
        super(circuit.getParty());
        this.circuit = circuit;
    }

    /**
     * 对于最后一轮的两个输入不满2的幂次时，得到包含填充数的输入
     *
     * @param first/second 两个输入的key
     * @return 包含dummy值
     */
    public MpcZ2Vector[] fromTwoSorted(MpcZ2Vector[] first, MpcZ2Vector[] second) throws MpcAbortException {
        preProcess(first, second);

        int level = LongUtils.ceilLog2(fixedTotalLen) - 1;
        for (int i = 0; i <= level; i++) {
            dealOneIterInLastLevel(level, i);
        }
        postProcess();

        return fixedInput;
    }

    private void preProcess(MpcZ2Vector[] first, MpcZ2Vector[] second) {
        originalLeftLen = first[0].bitNum();
        originalRightLen = second[0].bitNum();
        int halfMax = smallestPowerOfTwoBiggerEqualThan(Math.max(originalLeftLen, originalRightLen));
        fixedTotalLen = halfMax << 1;
        boolean noExtraBit = halfMax == originalLeftLen && originalLeftLen == originalRightLen;
        fixedInput = new MpcZ2Vector[noExtraBit ? first.length : first.length + 1];
        if (!noExtraBit) {
            // add a sign vector such that only the bit in the valid index is 1
            int oneNum = originalLeftLen + originalRightLen;
            BitVector middleIsOne = BitVectorFactory.createOnes(oneNum);
            middleIsOne.extendBitNum(fixedTotalLen);
            middleIsOne.fixShiftLefti(halfMax - originalRightLen);
            fixedInput[0] = party.setPublicValues(new BitVector[]{middleIsOne})[0];
        }
        int copyStartIndex = noExtraBit ? 0 : 1;
        IntStream intStream = party.getParallel() ? IntStream.range(0, first.length).parallel() : IntStream.range(0, first.length);
        intStream.forEach(dim -> {
            MpcZ2Vector secondTmp = (MpcZ2Vector) second[dim].copy();
            Arrays.stream(secondTmp.getBitVectors()).forEach(each -> each.extendBitNum(halfMax));
            secondTmp.reverseBits();
            fixedInput[dim + copyStartIndex] = (MpcZ2Vector) first[dim].copy();
            Arrays.stream(fixedInput[dim + copyStartIndex].getBitVectors()).forEach(each -> each.extendBitNum(halfMax));
            fixedInput[dim + copyStartIndex].merge(secondTmp);
        });
    }

    private void postProcess() {
        int halfMax = smallestPowerOfTwoBiggerEqualThan(Math.max(originalLeftLen, originalRightLen));
        boolean noExtraBit = halfMax == originalLeftLen && originalLeftLen == originalRightLen;
        if (!noExtraBit) {
            IntStream.range(1, fixedInput.length).forEach(i -> fixedInput[i].reduce(originalLeftLen + originalRightLen));
            fixedInput = Arrays.copyOfRange(fixedInput, 1, fixedInput.length);
        }
    }

    private void dealOneIterInLastLevel(int level, int iterNum) throws MpcAbortException {
        MathPreconditions.checkGreaterOrEqual("level >= iterNum", level, iterNum);
        int skipLen = 1 << (level - iterNum);
        int partLen = skipLen << 1;
        // how many parts should be sorted in this iteration. If the last part's length < skipLen, then we don't sort this part.
        int currentSortNum = fixedTotalLen / partLen + (fixedTotalLen % partLen > skipLen ? 1 : 0);
        // the difference between the length of the first part and skipLen, 0 <= lessCompareLen < skipLen
        int lessCompareLen = fixedTotalLen % partLen <= skipLen ? 0 : partLen - fixedTotalLen % partLen;
        // how many pairs of data should be compared
        int totalCompareNum = currentSortNum * skipLen - lessCompareLen;
        compareExchange(totalCompareNum, skipLen);
    }

    private void compareExchange(int totalCompareNum, int skipLen) throws MpcAbortException {
        MpcZ2Vector[] upperX = new MpcZ2Vector[fixedInput.length], belowX = new MpcZ2Vector[fixedInput.length];
        IntStream intStream = party.getParallel() ? IntStream.range(0, fixedInput.length).parallel() : IntStream.range(0, fixedInput.length);
        intStream.forEach(i -> {
            MpcZ2Vector[] tmp = fixedInput[i].getBitsWithSkip(totalCompareNum, skipLen);
            upperX[i] = tmp[0];
            belowX[i] = tmp[1];
        });
        // get the comparison result, if r = 1, switch two values
        MpcZ2Vector compFlag = party.not(circuit.leq(upperX, belowX));
        MpcZ2Vector[] flags = IntStream.range(0, fixedInput.length).mapToObj(i -> compFlag).toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] switchX = party.and(flags, party.xor(upperX, belowX));
        intStream = party.getParallel() ? IntStream.range(0, fixedInput.length).parallel() : IntStream.range(0, fixedInput.length);
        MpcZ2Vector[] extendSwitchX = intStream.mapToObj(i -> switchX[i].extendBitsWithSkip(fixedTotalLen, skipLen)).toArray(MpcZ2Vector[]::new);
        fixedInput = party.xor(extendSwitchX, fixedInput);
    }

    /**
     * 大于等于给定数的最小的2^n
     */
    public static int smallestPowerOfTwoBiggerEqualThan(int n) {
        int k = 1;
        while (k > 0 && k < n) {
            k = k << 1;
        }
        return k;
    }
}

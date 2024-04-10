package edu.alibaba.mpc4j.s3pc.abb3.basic.utils;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongMacVector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.replicate.TripletRpLongVector;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.IntStream;

/**
 * Utilities for shuffle or permute operation
 *
 * @author Feng Han
 * @date 2024/01/18
 */
public class ShuffleUtils {
    /**
     * 1. generate a random permutation, which can permute values into specific positions
     * 2. update the flag, if f[i] = true, y[i] = y[i-1]; else y[i] = x[i-1]
     * 3. get the second permutation, finish the switch network
     *
     * @param fun       the target is y[i] = x[fun[i]]
     * @param originLen the length of input
     * @param flag      storing the flag info in duplicated network
     * @return two permutation
     */
    public static int[][] get2PerForSwitch(int[] fun, int originLen, boolean[] flag) {
        int maxLen = Math.max(fun.length, originLen);
        int[] pai = new int[fun.length], rho = new int[fun.length];
        Arrays.fill(pai, maxLen);
        // int[]:
        // first dim - the count of appearance of this value;
        // second dim - the target position in the first permutation;
        // third dim - for generation of the second permutation
        int[][] info = new int[originLen][3];
        for (int item : fun) {
            info[item][0]++;
        }
        int currentPos = 0;
        for (int i = 0; i < originLen; i++) {
            if (info[i][0] > 0) {
                info[i][1] = currentPos;
                pai[currentPos] = i;
                flag[currentPos] = false;
                currentPos += info[i][0];
            }
        }
        int point = 0;
        for (int i = 0; i < fun.length; i++) {
            if (pai[i] == maxLen) {
                while (point < originLen && info[point][0] > 0) {
                    point++;
                }
                pai[i] = point;
                point++;
            }
            int[] index = info[fun[i]];
            rho[i] = index[1] + index[2];
            index[2]++;
        }
        return new int[][]{pai, rho};
    }

    /**
     * Refer to High-throughput secure three-party computation for malicious adversaries and an honest majority - protocol 2.13
     * <p>
     * 1. generate p = [0, 1, ..., n-1]
     * 2. switch the values based on randomArray[index] mod index
     */
    public static int[] permutationGeneration(int[] randomArray) {
        int[] pai = IntStream.range(0, randomArray.length).toArray();
        for (int i = randomArray.length - 1; i > 0; i--) {
            int targetIndex = Math.floorMod(randomArray[i], i + 1);
            if (targetIndex != i) {
                int tmp = pai[targetIndex];
                pai[targetIndex] = pai[i];
                pai[i] = tmp;
            }
        }
        return pai;
    }

    /**
     * check whether the input function is an injective function
     */
    public static void checkCorrectIngFun(int[] pai, int targetDim) throws MpcAbortException {
        HashSet<Integer> hashSet = new HashSet<>(pai.length);
        for (int i : pai) {
            if (i < 0 || i >= targetDim || hashSet.contains(i)) {
                throw new MpcAbortException("it is not a correct injective function：" + i);
            } else {
                hashSet.add(i);
            }
        }
    }

    /**
     * in the semi-honest version, the sigma2 can be as long as pai.length, which is smaller or equal than sigma1
     * in the malicious version, the length of sigma2 should be as long as sigma1
     *
     * @param pai         target function: output[i] = input[pai[i]]
     * @param sigma1      the first permutation
     * @param isMalicious whether the protocol is malicious secure
     * @return the first one is sigma2, the second one is sigma2·sigma1, whose top-pai.length elements are the same as pai
     */
    public static int[][] getSigma2(int[] pai, int[] sigma1, boolean isMalicious) throws MpcAbortException {
        checkCorrectIngFun(pai, sigma1.length);
        int[][] res = new int[2][];
        // 1. new permutation -> pai
        if (isMalicious && sigma1.length > pai.length) {
            res[1] = new int[sigma1.length];
            System.arraycopy(pai, 0, res[1], 0, pai.length);
            boolean[] flag = new boolean[sigma1.length];
            Arrays.stream(pai).forEach(x -> flag[x] = true);
            // 1.1 generate a new permutation
            SecureRandom secureRandom = new SecureRandom();
            int[] forGenExtendPai = IntStream.range(0, sigma1.length - pai.length).map(i -> secureRandom.nextInt()).toArray();
            int[] another = permutationGeneration(forGenExtendPai);
            // 1.2 traverse the elements of another from bottom to top, if it does not belong to pai, pad it to the bottom of pai
            int startIndex = 0;
            for (int one = 0; one < sigma1.length; one++) {
                if (!flag[one]) {
                    res[1][another[startIndex] + pai.length] = one;
                    startIndex++;
                }
            }
            Preconditions.checkArgument(startIndex == sigma1.length - pai.length);
        } else {
            res[1] = pai;
        }
        // 2. generate new sigma2, in order to make pai[i] = sigma1[sigma2[i]]
        int[] invS1 = invOfPermutation(sigma1);
        res[0] = Arrays.stream(res[1]).parallel().map(x -> invS1[x]).toArray();
        return res;
    }

    /**
     * get the inverse of a permutation
     */
    public static int[] invOfPermutation(int[] pai) throws MpcAbortException {
        checkCorrectIngFun(pai, pai.length);
        int[] invRes = new int[pai.length];
        IntStream.range(0, pai.length).parallel().forEach(i -> invRes[pai[i]] = i);
        return invRes;
    }

    /**
     * realize pai·input: output[i] = input[pai[i]]
     */
    public static int[] applyPermutation(int[] input, int[] pai) throws MpcAbortException {
        MathPreconditions.checkGreaterOrEqual("input.length >= pai.length", input.length, pai.length);
        checkCorrectIngFun(pai, input.length);
        return Arrays.stream(pai).map(j -> input[j]).toArray();
    }

    public static long[] applyPermutation(long[] input, int[] pai) throws MpcAbortException {
        MathPreconditions.checkGreaterOrEqual("input.length >= pai.length", input.length, pai.length);
        checkCorrectIngFun(pai, input.length);
        return Arrays.stream(pai).mapToLong(j -> input[j]).toArray();
    }

    public static BitVector[] applyPermutation(BitVector[] input, int[] pai) throws MpcAbortException {
        MathPreconditions.checkGreaterOrEqual("input.length >= pai.length", input.length, pai.length);
        checkCorrectIngFun(pai, input.length);
        return Arrays.stream(pai).mapToObj(j -> input[j]).toArray(BitVector[]::new);
    }

    public static TripletRpLongVector[] applyPermutation(TripletRpLongVector[] input, int[] pai) throws MpcAbortException {
        MathPreconditions.checkGreaterOrEqual("input.length >= pai.length", input.length, pai.length);
        checkCorrectIngFun(pai, input.length);
        return Arrays.stream(pai).mapToObj(j -> input[j]).toArray(TripletRpLongVector[]::new);
    }

    public static TripletRpZ2Vector[] applyPermutation(TripletRpZ2Vector[] input, int[] pai) throws MpcAbortException {
        MathPreconditions.checkGreaterOrEqual("input.length >= pai.length", input.length, pai.length);
        checkCorrectIngFun(pai, input.length);
        return Arrays.stream(pai).mapToObj(j -> input[j]).toArray(TripletRpZ2Vector[]::new);
    }



    /**
     * apply permutation on input matrix in rows, which means get out[i][j] = in[i][pai[j]]
     */
    private static long[][] applyPermutationToRowsNoCheck(long[][] input, int[] pai) {
        return IntStream.range(0, input.length).parallel().mapToObj(i ->
            Arrays.stream(pai).mapToLong(j -> input[i][j]).toArray()).toArray(long[][]::new);
    }

    private static LongVector[] applyPermutationToRowsNoCheck(LongVector[] input, int[] pai) {
        return Arrays.stream(input).parallel().map(each -> {
            long[] data = each.getElements();
            return LongVector.create(Arrays.stream(pai).mapToLong(j -> data[j]).toArray());
        }).toArray(LongVector[]::new);
    }

    private static TripletLongVector applyPermutationToRowsNoCheck(TripletLongVector input, int[] pai) {
        assert input instanceof TripletRpLongVector;
        long[][] innerValue = Arrays.stream(input.getVectors()).map(LongVector::getElements).toArray(long[][]::new);
        long[][] permValue = applyPermutationToRowsNoCheck(innerValue, pai);
        if (input instanceof TripletRpLongMacVector) {
            TripletRpLongMacVector data = (TripletRpLongMacVector) input;
            if (data.getMacIndex() > 0) {
                long[][] innerMac = Arrays.stream(data.getMacVec()).map(LongVector::getElements).toArray(long[][]::new);
                long[][] permMac = applyPermutationToRowsNoCheck(innerMac, pai);
                return TripletRpLongMacVector.create(data.getMacIndex(), permValue, permMac);
            } else {
                return TripletRpLongMacVector.create(permValue);
            }
        } else {
            return TripletRpLongVector.create(permValue);
        }
    }

    public static BitVector[] applyPermutationToRowsNoCheck(BitVector[] input, int[] pai) {
        byte[][] transRes = ZlDatabase.create(EnvType.STANDARD, true, input).getBytesData();
        return ZlDatabase.create(input.length, Arrays.stream(pai).mapToObj(i -> transRes[i]).toArray(byte[][]::new))
            .bitPartition(EnvType.STANDARD, true);
    }

    /**
     * apply permutation on input matrix in rows, which means get out[i][j] = in[i][pai[j]]
     */
    public static long[][] applyPermutationToRows(long[][] input, int[] pai) throws MpcAbortException {
        MathPreconditions.checkGreaterOrEqual("input[0].length >= pai.length", input[0].length, pai.length);
        checkCorrectIngFun(pai, input[0].length);
        return applyPermutationToRowsNoCheck(input, pai);
    }

    public static LongVector[] applyPermutationToRows(LongVector[] input, int[] pai) throws MpcAbortException {
        MathPreconditions.checkGreaterOrEqual("input[0].length >= pai.length", input[0].getNum(), pai.length);
        checkCorrectIngFun(pai, input[0].getNum());
        return applyPermutationToRowsNoCheck(input, pai);
    }

    public static TripletLongVector applyPermutationToRows(TripletLongVector input, int[] pai) throws MpcAbortException {
        MathPreconditions.checkGreaterOrEqual("input[0].getNum() >= pai.length", input.getNum(), pai.length);
        checkCorrectIngFun(pai, input.getNum());
        return applyPermutationToRowsNoCheck(input, pai);
    }

    public static TripletLongVector[] applyPermutationToRows(TripletLongVector[] input, int[] pai) throws MpcAbortException {
        MathPreconditions.checkGreaterOrEqual("input[0].getNum() >= pai.length", input[0].getNum(), pai.length);
        checkCorrectIngFun(pai, input[0].getNum());
        return IntStream.range(0, input.length).parallel().mapToObj(i ->
            applyPermutationToRowsNoCheck(input[i], pai)).toArray(TripletLongVector[]::new);
    }

    public static BitVector[] applyPermutationToRows(BitVector[] input, int[] pai) throws MpcAbortException {
        MathPreconditions.checkGreaterOrEqual("input[0].bitNum() >= pai.length", input[0].bitNum(), pai.length);
        for (BitVector bitVector : input) {
            MathPreconditions.checkEqual("input[i].bitNum()", "input[0].bitNum()", bitVector.bitNum(), input[0].bitNum());
        }
        checkCorrectIngFun(pai, pai.length);
        return applyPermutationToRowsNoCheck(input, pai);
    }

    public static TripletZ2Vector[] applyPermutationToRows(TripletZ2Vector[] input, int[] pai) throws MpcAbortException {
        for(TripletZ2Vector each : input){
            assert each instanceof TripletRpZ2Vector;
        }
        return applyPermutationToRows((TripletRpZ2Vector[]) input, pai);
    }

    public static TripletRpZ2Vector[] applyPermutationToRows(TripletRpZ2Vector[] input, int[] pai) throws MpcAbortException {
        MathPreconditions.checkGreaterOrEqual("input[0].bitNum() >= pai.length", input[0].bitNum(), pai.length);
        checkCorrectIngFun(pai, pai.length);
        BitVector[] vecInput = new BitVector[input.length << 1];
        IntStream.range(0, input.length).forEach(i -> {
            vecInput[i] = input[i].getBitVectors()[0];
            vecInput[i + input.length] = input[i].getBitVectors()[1];
        });
        BitVector[] vecOut = applyPermutationToRows(vecInput, pai);
        return IntStream.range(0, input.length).mapToObj(i ->
            TripletRpZ2Vector.create(vecOut[i], vecOut[i + input.length])).toArray(TripletRpZ2Vector[]::new);
    }
}

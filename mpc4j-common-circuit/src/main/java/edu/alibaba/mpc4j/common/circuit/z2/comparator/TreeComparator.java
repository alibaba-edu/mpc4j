package edu.alibaba.mpc4j.common.circuit.z2.comparator;

import edu.alibaba.mpc4j.common.circuit.z2.AbstractZ2Circuit;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * compare two values in tree-form
 *
 * @author Feng Han
 * @date 2025/2/27
 */
public class TreeComparator extends AbstractZ2Circuit implements Comparator {

    public TreeComparator(Z2IntegerCircuit circuit) {
        super(circuit.getParty());
    }

    @Override
    public MpcZ2Vector leq(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        checkInputs(xiArray, yiArray);
        return party.not(biggerThanParallel(xiArray, yiArray));
    }

    public MpcZ2Vector biggerThanParallel(MpcZ2Vector[] x, MpcZ2Vector[] y) throws MpcAbortException {
        if (x.length == 1) {
            return party.xor(x[0], party.and(x[0], y[0]));
        }
        // first step: get the equal bit and big bit by comparing each 2 neighbor bits (l1, l2) and (r1, r2)
        int rowLength = x.length;
        // 两个数先计算 x^(x&Y)，得到每一位的 x>y
        MpcZ2Vector[] xorRes = party.xor(x, y);
        int startIndex = rowLength % 2;
        int halfRowLen = rowLength >> 1;
        MpcZ2Vector[] bitBig = new MpcZ2Vector[(rowLength >> 1) + startIndex];
        MpcZ2Vector[] bitEq = new MpcZ2Vector[(rowLength >> 1) + startIndex];
        MpcZ2Vector[] leftAndInput = new MpcZ2Vector[rowLength - startIndex];
        MpcZ2Vector[] rightAndInput = new MpcZ2Vector[rowLength - startIndex];
        for(int i = 0; i < halfRowLen; i++){
            leftAndInput[i] = party.not(xorRes[2 * i + startIndex]);
            rightAndInput[i] = party.not(xorRes[2 * i + 1 + startIndex]);
            leftAndInput[i + halfRowLen] = xorRes[2 * i + 1 + startIndex];
            rightAndInput[i + halfRowLen] = x[2 * i + 1 + startIndex];
        }
        MpcZ2Vector[] andRes = party.and(leftAndInput, rightAndInput);
        MpcZ2Vector[] rXorAndL2 = Arrays.copyOfRange(andRes, halfRowLen, andRes.length);
        MpcZ2Vector[] l1 = IntStream.range(0, halfRowLen).mapToObj(i-> x[i * 2 + startIndex]).toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] xor1 = IntStream.range(0, halfRowLen).mapToObj(i-> xorRes[i * 2 + startIndex]).toArray(MpcZ2Vector[]::new);
        MpcZ2Vector[] bigResLevel1 = party.xor(rXorAndL2, party.and(xor1, party.xor(l1, rXorAndL2)));
        for(int i = 0; i < halfRowLen; i++){
            bitEq[i + startIndex] = andRes[i];
            bitBig[i + startIndex] = bigResLevel1[i];
        }
        if(startIndex == 1){
            bitEq[0] = party.not(xorRes[0]);
            bitBig[0] = party.xor(x[0], party.and(x[0], y[0]));
        }
        // 进行 log(K) 轮的乘法, 乘法数量为 2n
        int[][] number = this.parallelNumberGen(bitBig.length);
        for (int[] oneInt : number) {
            int start = oneInt.length & 1;
            int halfLen = oneInt.length >> 1;
            // EQ = l.EQ·r.EQ, Big = l.Big^(l.EQ·r.Big)
            MpcZ2Vector[] leftInput = new MpcZ2Vector[(halfLen << 1) - 1];
            MpcZ2Vector[] rightInput = new MpcZ2Vector[(halfLen << 1) - 1];
            for (int i = 0; i < halfLen; i++) {
                leftInput[i] = bitEq[oneInt[2 * i + start]];
                rightInput[i] = bitBig[oneInt[2 * i + 1 + start]];
                if (i < halfLen - 1) {
                    leftInput[i + halfLen] = bitEq[oneInt[2 * i + start]];
                    rightInput[i + halfLen] = bitEq[oneInt[2 * i + 1 + start]];
                }
            }
            MpcZ2Vector[] tmpAnd = party.and(leftInput, rightInput);
            for (int i = 0; i < halfLen; i++) {
                bitBig[oneInt[2 * i + start]] = party.xor(tmpAnd[i], bitBig[oneInt[2 * i + start]]);
                if (i < halfLen - 1) {
                    bitEq[oneInt[2 * i + start]] = tmpAnd[i + halfLen];
                }
            }
        }
        return bitBig[0];
    }

    /**
     * 根据维度得到指示运行的数组
     * 数组的用处是对于那些需要递归执行的算法，指示每一层的参与计算的数据是第几维的
     */
    public int[][] parallelNumberGen(int rowLength) {
        if(rowLength <= 1){
            return new int[0][];
        }
        int[][] number = new int[LongUtils.ceilLog2(rowLength)][];
        number[0] = IntStream.range(0, rowLength).toArray();
        for (int i = 1; i < number.length; i++) {
            int odd = number[i - 1].length & 1;
            int halfLen = number[i - 1].length >> 1;
            number[i] = new int[odd + halfLen];
            if (odd == 1) {
                number[i][0] = number[i - 1][0];
            }
            for (int j = 0; j < halfLen; j++) {
                number[i][j + odd] = number[i - 1][2 * j + odd];
            }
        }
        return number;
    }
}

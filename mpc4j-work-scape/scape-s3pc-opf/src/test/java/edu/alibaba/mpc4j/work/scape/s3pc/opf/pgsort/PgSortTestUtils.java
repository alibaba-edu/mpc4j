package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.utils.ShuffleUtils;
import org.junit.Assert;

import java.math.BigInteger;
import java.util.stream.IntStream;

/**
 * 3p sorting test utils.
 *
 * @author Feng Han
 * @date 2024/03/04
 */
public class PgSortTestUtils {
    /**
     * verify the sort is correct
     *
     * @param input  input data.
     * @param output output data.
     * @param pai    permutation.
     * @param stable stable or not.
     * @throws MpcAbortException the protocol failure abort exception.
     */
    public static void verify(BigInteger[] input, BigInteger[] output, int[] pai, boolean stable) throws MpcAbortException {
        MathPreconditions.checkEqual("input.length", "pai.length", input.length, pai.length);
        ShuffleUtils.checkCorrectIngFun(pai, pai.length);
        BigInteger[] shouldOutput = new BigInteger[input.length];
        IntStream.range(0, pai.length).forEach(i -> shouldOutput[pai[i]] = input[i]);
        for(int i = 1; i < pai.length; i++){
            Assert.assertTrue(shouldOutput[i - 1].compareTo(shouldOutput[i]) <= 0);
        }
        if(stable){
            int[] invPai = ShuffleUtils.invOfPermutation(pai);
            for(int i = 1; i < pai.length; i++){
                if(shouldOutput[i - 1].compareTo(shouldOutput[i]) == 0){
                    Assert.assertTrue(invPai[i - 1] < invPai[i]);
                }
            }
        }
        if(output != null){
            Assert.assertArrayEquals(shouldOutput, output);
        }
    }
}

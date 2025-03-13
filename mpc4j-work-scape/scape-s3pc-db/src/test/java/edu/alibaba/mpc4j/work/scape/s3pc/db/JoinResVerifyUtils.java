package edu.alibaba.mpc4j.work.scape.s3pc.db;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.junit.Assert;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * @author Feng Han
 * @date 2025/3/3
 */
public class JoinResVerifyUtils {
    /**
     * verify whether the (inner or full) general join result is correct
     *
     * @param leftPlain  left table
     * @param rightPlain right table
     * @param keyDim     key dimension
     * @param res2Plain  join result
     * @param leftHash   hashMap to store the left input database in {join_key : {rowIndex}} form
     * @param rightHash  hashMap to store the right input database in {join_key : {rowIndex}} form
     * @param innerLen   real inner join output size
     */
    public static void checkInner4General(BitVector[] leftPlain, BitVector[] rightPlain, int keyDim, BitVector[] res2Plain,
                                          TIntObjectHashMap<List<Integer>> leftHash, TIntObjectHashMap<List<Integer>> rightHash, int innerLen) {
        int leftValueDim = leftPlain.length - keyDim - 1;
        int rightValueDim = rightPlain.length - keyDim - 1;
        Assert.assertEquals(keyDim + leftValueDim + rightValueDim + 1, res2Plain.length);

        BigInteger[] leftOriginalPayload = leftValueDim > 0
            ? ZlDatabase.create(EnvType.STANDARD, true,
                Arrays.copyOfRange(leftPlain, keyDim, leftPlain.length - 1))
            .getBigIntegerData()
            : null;

        BigInteger[] rightOriginalPayload = rightValueDim > 0
            ? ZlDatabase.create(EnvType.STANDARD, true,
                Arrays.copyOfRange(rightPlain, keyDim, rightPlain.length - 1))
            .getBigIntegerData()
            : null;

        int[] resultKey = bitVectorTransToColumnIntArray(Arrays.copyOf(res2Plain, keyDim));
        BigInteger[] resLeftPayload = leftValueDim > 0
            ? ZlDatabase.create(EnvType.STANDARD, true,
                Arrays.copyOfRange(res2Plain, keyDim, keyDim + leftValueDim))
            .getBigIntegerData()
            : null;
        BigInteger[] resRightPayload = rightValueDim > 0
            ? ZlDatabase.create(EnvType.STANDARD, true,
                Arrays.copyOfRange(res2Plain, keyDim + leftValueDim, keyDim + leftValueDim + rightValueDim))
            .getBigIntegerData()
            : null;
        boolean[] EqSign = BinaryUtils.byteArrayToBinary(res2Plain[res2Plain.length - 1].getBytes(), res2Plain[0].bitNum());

        HashSet<String> hashSet = new HashSet<>();
        for (int j = 0; j < resultKey.length; j++) {
            if (EqSign[j]) {
                Preconditions.checkArgument(leftHash.contains(resultKey[j]));
                Preconditions.checkArgument(rightHash.contains(resultKey[j]));
                long tmpLeftIndex = -1L, tmpRightIndex = -1L;
                if(leftOriginalPayload != null){
                    for (int one : leftHash.get(resultKey[j])) {
                        if (resLeftPayload[j].compareTo(leftOriginalPayload[one]) == 0) {
                            tmpLeftIndex = one;
                            break;
                        }
                    }
                }else{
                    tmpLeftIndex = leftHash.get(resultKey[j]).get(0);
                }
                if(rightOriginalPayload != null){
                    for (int one : rightHash.get(resultKey[j])) {
                        if (resRightPayload[j].compareTo(rightOriginalPayload[one]) == 0) {
                            tmpRightIndex = one;
                            break;
                        }
                    }
                }else{
                    tmpRightIndex = rightHash.get(resultKey[j]).get(0);
                }
                Preconditions.checkArgument(tmpLeftIndex >= 0);
                Preconditions.checkArgument(tmpRightIndex >= 0);
                String tmp = tmpLeftIndex + "-" + tmpRightIndex;
                Preconditions.checkArgument(!hashSet.contains(tmp));
                hashSet.add(tmp);
            }
        }
        Preconditions.checkArgument(innerLen == hashSet.size());
    }

    /**
     * verify whether the semi join result is correct
     *
     * @param rightPlain right table
     * @param res2Plain  join result
     * @param leftHash   hashMap to store the left input database in {join_key : {rowIndex}} form
     */
    public static void checkSemiJoin(LongVector[] rightPlain, LongVector res2Plain, HashMap<Long, List<Integer>> leftHash) {
        Assert.assertEquals(res2Plain.getNum(), rightPlain[0].getNum());
        for (int i = 0; i < res2Plain.getNum(); i++) {
            Assert.assertEquals(res2Plain.getElement(i) == 1,
                (leftHash.containsKey(rightPlain[0].getElement(i)) && rightPlain[rightPlain.length - 1].getElement(i) == 1));
        }
    }

    /**
     * verify whether the semi join result is correct
     *
     * @param rightPlain right table
     * @param keyDim     key dimension
     * @param res2Plain  join result
     * @param leftHash   hashMap to store the left input database in {join_key : {rowIndex}} form
     */
    public static void checkSemiJoin(BitVector[] rightPlain, int keyDim, BitVector res2Plain, TIntObjectHashMap<List<Integer>> leftHash) {
        Assert.assertEquals(res2Plain.bitNum(), rightPlain[0].bitNum());
        int[] rightKey = bitVectorTransToColumnIntArray(Arrays.copyOf(rightPlain, keyDim));
        for (int i = 0; i < res2Plain.bitNum(); i++) {
            Assert.assertEquals(res2Plain.get(i), (leftHash.containsKey(rightKey[i]) && rightPlain[rightPlain.length - 1].get(i)));
        }
    }

    public static int[] bitVectorTransToColumnIntArray(BitVector[] bitVectors) {
        return Arrays.stream(
                ZlDatabase.create(EnvType.STANDARD, true, bitVectors)
                    .getBigIntegerData())
            .mapToInt(BigInteger::intValue).toArray();
    }
}

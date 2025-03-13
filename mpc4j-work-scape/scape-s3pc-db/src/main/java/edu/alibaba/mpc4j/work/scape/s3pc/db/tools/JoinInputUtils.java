package edu.alibaba.mpc4j.work.scape.s3pc.db.tools;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.IntStream;

/**
 * utils for join test
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public class JoinInputUtils {
    /**
     * generate test data for PkPk join protocol
     *
     * @param testNum  input data size
     * @param keyDim the dimension of join_key
     * @param valueDim dimension of payload attributes
     * @param dataHash hashMap to store the input database in {join_key : {rowIndex}} form
     * @return simulated input database
     */
    public static BitVector[] getBinaryInput4PkJoin(int testNum, int keyDim, int valueDim, TIntObjectHashMap<List<Integer>> dataHash) {
        return getBinaryInput4PkJoin(testNum, keyDim, valueDim, true, dataHash);
    }

    /**
     * generate test data for PkPk join protocol
     *
     * @param testNum  input data size
     * @param keyDim the dimension of join_key
     * @param valueDim dimension of payload attributes
     * @param withDummy with dummy rows
     * @param dataHash hashMap to store the input database in {join_key : {rowIndex}} form
     * @return simulated input database
     */
    public static BitVector[] getBinaryInput4PkJoin(int testNum, int keyDim, int valueDim, boolean withDummy, TIntObjectHashMap<List<Integer>> dataHash) {
        Preconditions.checkArgument(keyDim >= LongUtils.ceilLog2(testNum));
        SecureRandom secureRandom = new SecureRandom();
        int[] plainIntKey = new int[testNum];
        if(keyDim > LongUtils.ceilLog2(testNum)){
            int andNum = (1 << (LongUtils.ceilLog2(testNum) + 1)) - 1;
            TIntHashSet hashSet = new TIntHashSet();
            for (int i = 0; i < testNum; i++) {
                int tmp;
                while (true) {
                    tmp = secureRandom.nextInt() & andNum;
                    if (!hashSet.contains(tmp)) {
                        hashSet.add(tmp);
                        break;
                    }
                }
                plainIntKey[i] = tmp;
            }
            plainIntKey = Arrays.stream(plainIntKey).sorted().toArray();
        }else{
            plainIntKey = IntStream.range(0, testNum).toArray();
        }
        // valid input size
        int validNum = withDummy ? Math.max(secureRandom.nextInt(testNum), testNum >> 1) : testNum;
        for (int i = 0; i < validNum; i++) {
            if (!dataHash.containsKey(plainIntKey[i])) {
                dataHash.put(plainIntKey[i], new LinkedList<>());
            }
            dataHash.get(plainIntKey[i]).add(i);
        }

        ZlDatabase zlDatabase = ZlDatabase.create(32, Arrays.stream(plainIntKey).mapToObj(IntUtils::intToByteArray).toArray(byte[][]::new));
        BitVector[] transBitVec = zlDatabase.bitPartition(EnvType.STANDARD_JDK, true);

        BitVector[] resultDb = new BitVector[keyDim + valueDim + 1];
        // copy key
        int i = 0;
        for (; i < keyDim - 32; i++) {
            resultDb[i] = BitVectorFactory.createZeros(testNum);
        }
        int copyStartPos = Math.max(0, 32 - keyDim);
        System.arraycopy(transBitVec, copyStartPos, resultDb, i, keyDim - i);
        // get payload
        i = keyDim;
        for (; i < resultDb.length - 1; i++) {
            resultDb[i] = BitVectorFactory.createRandom(testNum, secureRandom);
        }
        resultDb[resultDb.length - 1] = BitVectorFactory.createOnes(testNum);
        resultDb[resultDb.length - 1].fixShiftLefti(testNum - validNum);
        return resultDb;
    }

    /**
     * generate test data for general join protocol
     *
     * @param testNum   input data size
     * @param valueDim  dimension of payload attributes
     * @param freqBound the upper bound of the number of occurrences of each join_key
     * @param dataHash  hashMap to store the input database in {join_key : rowIndex} form
     * @return simulated input database
     */
    public static long[][] getInput4GeneralJoin(int testNum, int valueDim, int freqBound, HashMap<Long, List<Integer>> dataHash) {
        SecureRandom secureRandom = new SecureRandom();
        long andNum = (1L << LongUtils.ceilLog2(testNum)) - 1L;
        long[][] plainDb = new long[2 + valueDim][testNum];
        plainDb[valueDim + 1] = new long[testNum];
        // valid input size
        int validNum = Math.max(secureRandom.nextInt(testNum), testNum >> 1);
        Arrays.fill(plainDb[valueDim + 1], 0, validNum, 1L);

        HashMap<Long, int[]> hashMap = new HashMap<>();
        for (int i = 0; i < testNum; i++) {
            long tmp;
            while (true) {
                tmp = secureRandom.nextLong() & andNum;
                if (hashMap.containsKey(tmp)) {
                    if (hashMap.get(tmp)[0] < freqBound) {
                        hashMap.get(tmp)[0] += 1;
                        break;
                    }
                } else {
                    hashMap.put(tmp, new int[]{1});
                    break;
                }
            }
            plainDb[0][i] = tmp;
        }
        plainDb[0] = Arrays.stream(plainDb[0]).sorted().toArray();
        for (int i = 0; i < testNum; i++) {
            if (i == 0 || plainDb[0][i] != plainDb[0][i - 1]) {
                for (int j = 1; j <= valueDim; j++) {
                    plainDb[j][i] = secureRandom.nextLong();
                }
            } else {
                for (int j = 1; j <= valueDim; j++) {
                    plainDb[j][i] = plainDb[j][i - 1] + 1L;
                }
            }
            if (i < validNum) {
                if (!dataHash.containsKey(plainDb[0][i])) {
                    dataHash.put(plainDb[0][i], new LinkedList<>());
                }
                dataHash.get(plainDb[0][i]).add(i);
            }
        }
        return plainDb;
    }

    /**
     * get the valid output size of an inner join
     *
     * @param leftHash  hashMap to store the left input database in {join_key : {rowIndex}} form
     * @param rightHash hashMap to store the right input database in {join_key : {rowIndex}} form
     * @return the valid output size of an inner join
     */
    public static int getRealInner4GeneralJoin(HashMap<Long, List<Integer>> leftHash, HashMap<Long, List<Integer>> rightHash) {
        int real = 0;
        for (long key : leftHash.keySet()) {
            if (rightHash.containsKey(key)) {
                real += leftHash.get(key).size() * rightHash.get(key).size();
            }
        }
        return real;
    }

    /**
     * get the valid output size of an inner join
     *
     * @param leftHash  hashMap to store the left input database in {join_key : {rowIndex}} form
     * @param rightHash hashMap to store the right input database in {join_key : {rowIndex}} form
     * @return the valid output size of an inner join
     */
    public static int getRealInner4GeneralJoin(TIntObjectHashMap<List<Integer>> leftHash, TIntObjectHashMap<List<Integer>> rightHash) {
        int real = 0;
        for (int key : leftHash.keys()) {
            if (rightHash.containsKey(key)) {
                real += leftHash.get(key).size() * rightHash.get(key).size();
            }
        }
        return real;
    }

    /**
     * verify whether the (inner or full) general join result is correct
     *
     * @param leftPlain left table
     * @param rightPlain right table
     * @param leftValue the payload dimension of the left table
     * @param rightValue the payload dimension of the right table
     * @param res2Plain join result
     * @param leftHash  hashMap to store the left input database in {join_key : {rowIndex}} form
     * @param rightHash hashMap to store the right input database in {join_key : {rowIndex}} form
     * @param innerLen real inner join output size
     */
    public static void checkInner4General(long[][] leftPlain, long[][] rightPlain, int leftValue, int rightValue, long[][] res2Plain,
                                          HashMap<Long, List<Integer>> leftHash, HashMap<Long, List<Integer>> rightHash, int innerLen) {
        HashSet<String> hashSet = new HashSet<>(leftPlain[0].length);
        int addNum = 1;
        int testIndex = res2Plain.length - 1;
        for (int j = 0; j < res2Plain[0].length; j++) {
            if (res2Plain[testIndex][j] == 1L) {
                Preconditions.checkArgument(leftHash.containsKey(res2Plain[0][j]));
                Preconditions.checkArgument(rightHash.containsKey(res2Plain[0][j]));
                long tmpLeftIndex = -1L, tmpRightIndex = -1L;
                for (int one : leftHash.get(res2Plain[0][j])) {
                    if (res2Plain[1][j] == leftPlain[1][one]) {
                        for (int k = 1; k < leftValue; k++) {
                            Preconditions.checkArgument(res2Plain[1 + k][j] == leftPlain[1 + k][one]);
                        }
                        tmpLeftIndex = one;
                        break;
                    }
                }
                for (int one : rightHash.get(res2Plain[0][j])) {
                    if (res2Plain[addNum + leftValue][j] == rightPlain[1][one]) {
                        for (int k = 1; k < rightValue; k++) {
                            Preconditions.checkArgument(res2Plain[addNum + leftValue + k][j] == rightPlain[1 + k][one]);
                        }
                        tmpRightIndex = one;
                        break;
                    }
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
}


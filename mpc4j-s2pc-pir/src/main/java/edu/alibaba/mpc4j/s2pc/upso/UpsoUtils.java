package edu.alibaba.mpc4j.s2pc.upso;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.NoStashCuckooHashBin;
import edu.alibaba.mpc4j.common.tool.polynomial.power.PowerNode;
import edu.alibaba.mpc4j.common.tool.polynomial.power.PowerUtils;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64Poly;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * UPSO utils.
 *
 * @author Liqiang Peng
 * @date 2023/7/24
 */
public class UpsoUtils {

    /**
     * return encoded array.
     *
     * @param hashBinEntry hash bin entry.
     * @param shiftMask    shift mask.
     * @return encoded array.
     */
    public static long getHashBinEntryEncodedArray(byte[] hashBinEntry, BigInteger shiftMask) {
        BigInteger input = BigIntegerUtils.byteArrayToNonNegBigInteger(hashBinEntry);
        return input.and(shiftMask).longValueExact();
    }

    /**
     * return encoded array.
     *
     * @param hashBinEntry        hash bin entry.
     * @param shiftMask           shift mask.
     * @param itemEncodedSlotSize item encoded slot size.
     * @param plainModulusSize    plain modulus size.
     * @return encoded array.
     */
    public static long[] getHashBinEntryEncodedArray(byte[] hashBinEntry, BigInteger shiftMask, int itemEncodedSlotSize,
                                                     int plainModulusSize) {
        long[] encodedArray = new long[itemEncodedSlotSize];
        BigInteger input = BigIntegerUtils.byteArrayToNonNegBigInteger(hashBinEntry);
        for (int i = 0; i < itemEncodedSlotSize; i++) {
            encodedArray[i] = input.and(shiftMask).longValueExact();
            input = input.shiftRight(plainModulusSize);
        }
        return encodedArray;
    }

    /**
     * encode query.
     *
     * @param inputArray        input byte array.
     * @param itemPerCiphertext item per ciphertext.
     * @param ciphertextNum     ciphertext num.
     * @param polyModulusDegree poly modulus degree.
     * @param shiftMask         shift mask.
     * @return encoded array.
     */
    public static long[][] encodeQuery(byte[][] inputArray, int itemPerCiphertext, int ciphertextNum,
                                       int polyModulusDegree, BigInteger shiftMask, long plainModulus,
                                       SecureRandom secureRandom) {
        long[][] items = IntStream.range(0, ciphertextNum)
            .mapToObj(i -> IntStream.range(0, polyModulusDegree)
                .mapToLong(l -> Math.abs(secureRandom.nextLong()) % plainModulus)
                .toArray())
            .toArray(long[][]::new);
        for (int i = 0; i < ciphertextNum; i++) {
            for (int j = 0; j < itemPerCiphertext; j++) {
                items[i][j] = getHashBinEntryEncodedArray(inputArray[i * itemPerCiphertext + j], shiftMask);
            }
        }
        return items;
    }

    /**
     * encode query.
     *
     * @param cuckooHashBin     cuckoo hash bin.
     * @param itemPerCiphertext item per ciphertext.
     * @param ciphertextNum     ciphertext num.
     * @param polyModulusDegree poly modulus degree.
     * @param shiftMask         shift mask.
     * @return encoded array.
     */
    public static long[][] encodeQuery(NoStashCuckooHashBin<byte[]> cuckooHashBin, int itemPerCiphertext,
                                       int ciphertextNum, int polyModulusDegree, BigInteger shiftMask) {
        long[][] items = IntStream.range(0, ciphertextNum)
            .mapToObj(i -> IntStream.range(0, polyModulusDegree)
                .mapToLong(l -> 2L)
                .toArray())
            .toArray(long[][]::new);
        for (int i = 0; i < ciphertextNum; i++) {
            for (int j = 0; j < itemPerCiphertext; j++) {
                HashBinEntry<byte[]> hashBinEntry = cuckooHashBin.getHashBinEntry(i * itemPerCiphertext + j);
                if (hashBinEntry.getHashIndex() != HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                    items[i][j] = getHashBinEntryEncodedArray(hashBinEntry.getItem(), shiftMask);
                }
            }
        }
        return items;
    }

    /**
     * compute powers.
     *
     * @param base      base.
     * @param zp64      zp64.
     * @param exponents exponent array.
     * @param parallel  parallel.
     * @return powers.
     */
    public static long[][] computePowers(long[] base, Zp64 zp64, int[] exponents, boolean parallel) {
        assert exponents[0] == 1;
        long[][] result = new long[exponents.length][base.length];
        result[0] = base;
        IntStream intStream = IntStream.range(1, exponents.length);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(i ->
            IntStream.range(0, base.length).forEach(j -> result[i][j] = zp64.pow(base[j], exponents[i]))
        );
        return result;
    }

    /**
     * encode database.
     *
     * @param hashBins  hash bin.
     * @param binSize   bin size.
     * @param binNum    bin num.
     * @param shiftMask shift mask.
     * @return encoded database.
     */
    public static long[][] encodeDatabase(byte[][][] hashBins, int binSize, int binNum, BigInteger shiftMask,
                                          long plainModulus, SecureRandom secureRandom) {
        long[][] encodedItemArray = new long[binNum][binSize];
        for (int i = 0; i < binNum; i++) {
            for (int j = 0; j < hashBins[i].length; j++) {
                encodedItemArray[i][j] = getHashBinEntryEncodedArray(hashBins[i][j], shiftMask);
            }
            // padding dummy elements
            for (int j = 0; j < binSize - hashBins[i].length; j++) {
                encodedItemArray[i][j + hashBins[i].length] = Math.abs(secureRandom.nextLong()) % plainModulus;
            }
        }
        return encodedItemArray;
    }

    /**
     * polynomial root interpolate.
     * @param rootArrays             root array.
     * @param itemPerCiphertext      item per ciphertext.
     * @param ciphertextNum          ciphertext num.
     * @param alpha                  alpha.
     * @param maxPartitionSizePerBin max partition size per bin.
     * @param polyModulusDegree      poly modulus degree.
     * @param itemEncodedSlotSize    item encoded slot size.
     * @param zp64Poly               z64 poly.
     * @param parallel               parallel.
     * @return coeffs.
     */
    public static List<long[][]> rootInterpolate(long[][] rootArrays, int itemPerCiphertext, int ciphertextNum,
                                                 int alpha, int maxPartitionSizePerBin, int polyModulusDegree,
                                                 int itemEncodedSlotSize, Zp64Poly zp64Poly, boolean parallel) {
        long[][] coeffs = new long[itemEncodedSlotSize * itemPerCiphertext][];
        List<long[][]> coeffsPolys = new ArrayList<>();
        // for each bucket, compute the coefficients of the polynomial f(x) = \prod_{y in bucket} (x - y)
        for (int i = 0; i < ciphertextNum; i++) {
            for (int partition = 0; partition < alpha; partition++) {
                int partitionStart = maxPartitionSizePerBin * partition;
                IntStream intStream = IntStream.range(0, itemPerCiphertext * itemEncodedSlotSize);
                intStream = parallel ? intStream.parallel() : intStream;
                int finalI = i;
                intStream.forEach(j -> {
                    long[] rootArray = new long[maxPartitionSizePerBin];
                    System.arraycopy(
                        rootArrays[finalI * itemPerCiphertext * itemEncodedSlotSize + j],
                        partitionStart,
                        rootArray,
                        0,
                        maxPartitionSizePerBin
                    );
                    coeffs[j] = zp64Poly.rootInterpolate(maxPartitionSizePerBin, rootArray, 0L);
                });
                // transpose
                long[][] temp = new long[maxPartitionSizePerBin + 1][polyModulusDegree];
                for (int j = 0; j < maxPartitionSizePerBin + 1; j++) {
                    for (int l = 0; l < itemPerCiphertext * itemEncodedSlotSize; l++) {
                        temp[j][l] = coeffs[l][j];
                    }
                    for (int l = itemPerCiphertext * itemEncodedSlotSize; l < polyModulusDegree; l++) {
                        temp[j][l] = 0;
                    }
                }
                coeffsPolys.add(temp);
            }
        }
        return coeffsPolys;
    }

    /**
     * compute power degree.
     *
     * @param queryPowers            query powers.
     * @param maxPartitionSizePerBin max partition size per bin.
     * @return power degree.
     */
    public static int[][] computePowerDegree(int[] queryPowers, int maxPartitionSizePerBin) {
        int[][] powerDegree;
        Set<Integer> sourcePowersSet = Arrays.stream(queryPowers)
            .boxed()
            .collect(Collectors.toCollection(HashSet::new));
        PowerNode[] powerNodes = PowerUtils.computePowers(sourcePowersSet, maxPartitionSizePerBin);
        powerDegree = Arrays.stream(powerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
        return powerDegree;
    }

    /**
     * vector array OR operation.
     *
     * @param array    vector array.
     * @param z2cParty z2c party.
     * @return or vector of input vectors.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public static MpcZ2Vector or(MpcZ2Vector[] array, Z2cParty z2cParty) throws MpcAbortException {
        int l = array.length;
        // tree-based AND
        int logL = LongUtils.ceilLog2(l);
        for (int h = 1; h <= logL; h++) {
            int nodeNum = array.length / 2;
            MpcZ2Vector[] eqXiArray = new MpcZ2Vector[nodeNum];
            MpcZ2Vector[] eqYiArray = new MpcZ2Vector[nodeNum];
            for (int i = 0; i < nodeNum; i++) {
                eqXiArray[i] = array[i * 2];
                eqYiArray[i] = array[i * 2 + 1];
            }
            MpcZ2Vector[] eqZiArray = z2cParty.or(eqXiArray, eqYiArray);
            if (array.length % 2 == 1) {
                eqZiArray = Arrays.copyOf(eqZiArray, nodeNum + 1);
                eqZiArray[nodeNum] = array[array.length - 1];
            }
            array = eqZiArray;
        }
        return array[0];
    }

    /**
     * compute power degree.
     *
     * @param psLowDegree            Paterson-Stockmeyer low degree.
     * @param queryPowers            query powers.
     * @param maxPartitionSizePerBin max partition size per bin.
     * @return power degree.
     */
    public static int[][] computePowerDegree(int psLowDegree, int[] queryPowers, int maxPartitionSizePerBin) {
        int[][] powerDegree;
        if (psLowDegree > 0) {
            Set<Integer> innerPowersSet = new HashSet<>();
            Set<Integer> outerPowersSet = new HashSet<>();
            IntStream.range(0, queryPowers.length).forEach(i -> {
                if (queryPowers[i] <= psLowDegree) {
                    innerPowersSet.add(queryPowers[i]);
                } else {
                    outerPowersSet.add(queryPowers[i] / (psLowDegree + 1));
                }
            });
            PowerNode[] innerPowerNodes = PowerUtils.computePowers(innerPowersSet, psLowDegree);
            PowerNode[] outerPowerNodes = PowerUtils.computePowers(
                outerPowersSet, maxPartitionSizePerBin / (psLowDegree + 1));
            powerDegree = new int[innerPowerNodes.length + outerPowerNodes.length][2];
            int[][] innerPowerNodesDegree = Arrays.stream(innerPowerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
            int[][] outerPowerNodesDegree = Arrays.stream(outerPowerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
            System.arraycopy(innerPowerNodesDegree, 0, powerDegree, 0, innerPowerNodesDegree.length);
            System.arraycopy(outerPowerNodesDegree, 0, powerDegree, innerPowerNodesDegree.length, outerPowerNodesDegree.length);
        } else {
            Set<Integer> sourcePowersSet = Arrays.stream(queryPowers)
                .boxed()
                .collect(Collectors.toCollection(HashSet::new));
            PowerNode[] powerNodes = PowerUtils.computePowers(sourcePowersSet, maxPartitionSizePerBin);
            powerDegree = Arrays.stream(powerNodes).map(PowerNode::toIntArray).toArray(int[][]::new);
        }
        return powerDegree;
    }
}

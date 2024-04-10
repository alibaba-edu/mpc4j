package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.polynomial.power.PowersDag;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * CMG21 keyword PIR params checker.
 *
 * @author Liqiang Peng
 * @date 2022/8/9
 */
public class Cmg21KwPirParamsChecker {
    /**
     * private constructor.
     */
    private Cmg21KwPirParamsChecker() {
        // empty
    }

    /**
     * check the validity of keyword PIR params.
     *
     * @param params params.
     * @return whether the keyword PIR params is valid.
     */
    public static boolean checkValid(Cmg21KwPirParams params) {
        assert params.getCuckooHashBinType().equals(CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH)
            || params.getCuckooHashBinType().equals(CuckooHashBinFactory.CuckooHashBinType.NO_STASH_ONE_HASH)
            : CuckooHashBinFactory.CuckooHashBinType.class.getSimpleName() + "only support "
            + CuckooHashBinFactory.CuckooHashBinType.NO_STASH_ONE_HASH + " or "
            + CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH;
        assert params.getBinNum() > 0 : "bin num must be greater than 0: " + params.getBinNum();
        assert params.getItemEncodedSlotSize() >= 2 && params.getItemEncodedSlotSize() <= 32
            : "ItemEncodedSlotSize must be in range [2, 32]: " + params.getItemEncodedSlotSize();
        assert params.getPsLowDegree() <= params.getMaxPartitionSizePerBin()
            : "psLowDegree should be smaller or equal than maxPartitionSizePerBin";
        // check query powers
        checkQueryPowers(params.getQueryPowers(), params.getPsLowDegree());
        assert (params.getPolyModulusDegree() & (params.getPolyModulusDegree() - 1)) == 0 :
            "polyModulusDegree is not a power of two";
        assert params.getPlainModulus() % (2L * params.getPolyModulusDegree()) == 1 :
            "plainModulus should be a specific prime number to supports batching";
        int encodedBitLength = params.getItemEncodedSlotSize() *
            (int) Math.floor(Math.log(params.getPlainModulus()) / Math.log(2));
        assert encodedBitLength >= 80 && encodedBitLength <= 128 : "encoded bits should greater than or equal 80 " +
            "and smaller than or equal 128";
        assert params.getBinNum() % (params.getPolyModulusDegree() / params.getItemEncodedSlotSize()) == 0 :
            "binNum should be a multiple of polyModulusDegree / itemEncodedSlotSize";
        assert params.expectServerSize() > 0 : "ExpectServerSize must be greater than 0: " + params.expectServerSize();
        int maxItemSize = CuckooHashBinFactory.getMaxItemSize(params.getCuckooHashBinType(), params.getBinNum());
        assert params.maxRetrievalSize() > 0 && params.maxRetrievalSize() <= maxItemSize
            : "MaxRetrievalSize must be in range (0, " + maxItemSize + "]: " + params.maxRetrievalSize();
        int[][] parentPowers;
        if (params.getPsLowDegree() > 0) {
            int queryPowerNum = params.getQueryPowers().length;
            TIntSet innerPowersSet = new TIntHashSet(queryPowerNum);
            TIntSet outerPowersSet = new TIntHashSet(queryPowerNum);
            IntStream.range(0, queryPowerNum).forEach(i -> {
                if (params.getQueryPowers()[i] <= params.getPsLowDegree()) {
                    innerPowersSet.add(params.getQueryPowers()[i]);
                } else {
                    outerPowersSet.add(params.getQueryPowers()[i] / (params.getPsLowDegree() + 1));
                }
            });
            PowersDag innerPowersDag = new PowersDag(innerPowersSet, params.getPsLowDegree());
            PowersDag outerPowersDag = new PowersDag(
                outerPowersSet, params.getMaxPartitionSizePerBin() / (params.getPsLowDegree() + 1)
            );
            parentPowers = new int[innerPowersDag.upperBound() + outerPowersDag.upperBound()][2];
            int[][] innerPowerNodesDegree = innerPowersDag.getDag();
            int[][] outerPowerNodesDegree = outerPowersDag.getDag();
            System.arraycopy(innerPowerNodesDegree, 0, parentPowers, 0, innerPowerNodesDegree.length);
            System.arraycopy(
                outerPowerNodesDegree, 0, parentPowers, innerPowerNodesDegree.length, outerPowerNodesDegree.length
            );
        } else {
            TIntSet sourcePowersSet = new TIntHashSet(params.getQueryPowers());
            PowersDag powersDag = new PowersDag(sourcePowersSet, params.getMaxPartitionSizePerBin());
            parentPowers = powersDag.getDag();
        }
        return Cmg21KwPirNativeUtils.checkSealParams(
            params.getPolyModulusDegree(), params.getPlainModulus(), params.getCoeffModulusBits(), parentPowers,
            params.getQueryPowers(), params.getPsLowDegree(), params.getMaxPartitionSizePerBin()
        );
    }

    /**
     * check the validity of query powers.
     *
     * @param sourcePowers source powers.
     * @param psLowDegree  Paterson-Stockmeyer low degree.
     */
    private static void checkQueryPowers(int[] sourcePowers, int psLowDegree) {
        int[] sortSourcePowers = Arrays.stream(sourcePowers)
            .peek(sourcePower -> {
                assert sourcePower > 0 : "query power must be greater than 0: " + sourcePower;
            })
            .distinct()
            .sorted()
            .toArray();
        assert sortSourcePowers.length == sourcePowers.length : "query powers must be distinct";
        assert sortSourcePowers[0] == 1 : "query powers must contain 1";
        for (int sourcePower : sourcePowers) {
            assert sourcePower <= psLowDegree || sourcePower % (psLowDegree + 1) == 0
                : "query powers should be divided by ps_low_degree + 1 or smaller than ps_low_degree: " + sourcePower;
        }
    }
}

package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21j;

import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;

import java.util.Arrays;

/**
 * CMG21J UPSI params checker.
 *
 * @author Liqiang Peng
 * @date 2022/8/9
 */
public class Cmg21jUpsiParamsChecker {

    private Cmg21jUpsiParamsChecker() {
        // empty
    }

    /**
     * check the validity of the params.
     *
     * @param params params.
     * @return whether the params is valid.
     */
    public static boolean checkValid(Cmg21jUpsiParams params) {
        assert params.getCuckooHashBinType().equals(CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH)
            || params.getCuckooHashBinType().equals(CuckooHashBinFactory.CuckooHashBinType.NO_STASH_ONE_HASH)
            : CuckooHashBinFactory.CuckooHashBinType.class.getSimpleName() + "only support "
            + CuckooHashBinFactory.CuckooHashBinType.NO_STASH_ONE_HASH + " or "
            + CuckooHashBinFactory.CuckooHashBinType.NAIVE_3_HASH;
        assert params.getBinNum() > 0 : "bin num should be greater than 0";
        assert params.getItemEncodedSlotSize() >= 2 && params.getItemEncodedSlotSize() <= 32
            : "the size of slots for encoded item should smaller than or equal 32 and greater than or equal 2";
        assert params.getPsLowDegree() <= params.getMaxPartitionSizePerBin() :
            "psLowDegree should be smaller or equal than maxPartitionSizePerBin";
        checkQueryPowers(params.getQueryPowers(), params.getPsLowDegree());
        assert (params.getPolyModulusDegree() & (params.getPolyModulusDegree() - 1)) == 0
            : "polyModulusDegree is not a power of two";
        assert params.getPlainModulus() % (2L * params.getPolyModulusDegree()) == 1
            : "plainModulus should be a specific prime number to supports batching";
        int encodedBitLength = params.getItemEncodedSlotSize() *
            (int) Math.floor(Math.log(params.getPlainModulus()) / Math.log(2));
        assert encodedBitLength >= 80 && encodedBitLength <= 256
            : "encoded bits should greater than or equal 80 and smaller than or equal 256";
        assert params.getBinNum() % (params.getPolyModulusDegree() / params.getItemEncodedSlotSize()) == 0
            : "binNum should be a multiple of polyModulusDegree / itemEncodedSlotSize";
        assert params.expectServerSize() > 0 : "ExpectServerSize must be greater than 0: " + params.expectServerSize();
        int maxItemSize = CuckooHashBinFactory.getMaxItemSize(params.getCuckooHashBinType(), params.getBinNum());
        assert params.maxClientElementSize() > 0 && params.maxClientElementSize() <= maxItemSize
            : "MaxClientElementSize must be in range (0, " + maxItemSize + "]: " + params.maxClientElementSize();
        return true;
    }

    /**
     * check the validity of the query powers.
     *
     * @param sourcePowers source powers.
     * @param psLowDegree  Paterson-Stockmeyer low degree
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
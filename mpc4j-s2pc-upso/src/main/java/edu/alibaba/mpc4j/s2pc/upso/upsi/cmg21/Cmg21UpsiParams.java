package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21;

import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiParams;

import java.util.Arrays;

/**
 * CMG21 UPSI params.
 *
 * @author Liqiang Peng
 * @date 2022/5/25
 */
public class Cmg21UpsiParams implements UpsiParams {
    /**
     * cuckoo hash type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * bin num
     */
    private final int binNum;
    /**
     * max partition size per bin
     */
    private final int maxPartitionSizePerBin;
    /**
     * item encoded slot size
     */
    private final int itemEncodedSlotSize;
    /**
     * Paterson-Stockmeyer low degree
     */
    private final int psLowDegree;
    /**
     * query powers
     */
    private final int[] queryPowers;
    /**
     * plain modulus
     */
    private final long plainModulus;
    /**
     * poly modulus degree
     */
    private final int polyModulusDegree;
    /**
     * coeff modulus bits
     */
    private final int[] coeffModulusBits;
    /**
     * expect server size
     */
    private final int expectServerSize;
    /**
     * max client size
     */
    private final int maxClientSize;
    /**
     * item per ciphertext
     */
    private final int itemPerCiphertext;
    /**
     * ciphertext num
     */
    private final int ciphertextNum;

    private Cmg21UpsiParams(CuckooHashBinType cuckooHashBinType, int binNum,
                            int maxPartitionSizePerBin, int itemEncodedSlotSize, int psLowDegree,
                            int[] queryPowers, long plainModulus, int polyModulusDegree,
                            int[] coeffModulusBits, int expectServerSize, int maxClientSize) {
        this.cuckooHashBinType = cuckooHashBinType;
        this.binNum = binNum;
        this.maxPartitionSizePerBin = maxPartitionSizePerBin;
        this.itemEncodedSlotSize = itemEncodedSlotSize;
        this.psLowDegree = psLowDegree;
        this.queryPowers = queryPowers;
        this.plainModulus = plainModulus;
        this.polyModulusDegree = polyModulusDegree;
        this.coeffModulusBits = coeffModulusBits;
        this.expectServerSize = expectServerSize;
        this.maxClientSize = maxClientSize;
        this.itemPerCiphertext = polyModulusDegree / itemEncodedSlotSize;
        this.ciphertextNum = binNum / itemPerCiphertext;
    }

    /**
     * create UPSI params without checking the validity of the params.
     *
     * @param cuckooHashBinType      cuckoo hash type.
     * @param binNum                 bin num.
     * @param maxPartitionSizePerBin max partition size per bin.
     * @param itemEncodedSlotSize    item encoded slot size.
     * @param psLowDegree            Paterson-Stockmeyer low degree.
     * @param queryPowers            query powers.
     * @param plainModulus           plain modulus.
     * @param polyModulusDegree      poly modulus degree.
     * @param coeffModulusBits       coeff modulus bits.
     * @param expectServerSize       expect server size.
     * @param maxClientSize          max client size.
     * @return UPSI params.
     */
    public static Cmg21UpsiParams uncheckCreate(CuckooHashBinType cuckooHashBinType, int binNum,
                                                int maxPartitionSizePerBin, int itemEncodedSlotSize, int psLowDegree,
                                                int[] queryPowers, long plainModulus, int polyModulusDegree,
                                                int[] coeffModulusBits, int expectServerSize, int maxClientSize) {
        return new Cmg21UpsiParams(
            cuckooHashBinType, binNum, maxPartitionSizePerBin,
            itemEncodedSlotSize, psLowDegree, queryPowers,
            plainModulus, polyModulusDegree, coeffModulusBits,
            expectServerSize, maxClientSize
        );
    }

    /**
     * create a valid UPSI params.
     *
     * @param cuckooHashBinType      cuckoo hash type.
     * @param binNum                 bin num.
     * @param maxPartitionSizePerBin max partition size per bin.
     * @param itemEncodedSlotSize    item encoded slot size.
     * @param psLowDegree            Paterson-Stockmeyer low degree.
     * @param queryPowers            query powers.
     * @param plainModulus           plain modulus.
     * @param polyModulusDegree      poly modulus degree.
     * @param coeffModulusBits       coeff modulus bits.
     * @param expectServerSize       expect server size.
     * @param maxClientSize          max client size.
     * @return UPSI params.
     */
    public static Cmg21UpsiParams create(CuckooHashBinType cuckooHashBinType, int binNum,
                                         int maxPartitionSizePerBin, int itemEncodedSlotSize, int psLowDegree,
                                         int[] queryPowers, long plainModulus, int polyModulusDegree,
                                         int[] coeffModulusBits, int expectServerSize, int maxClientSize) {
        Cmg21UpsiParams cmg21UpsiParams = uncheckCreate(
            cuckooHashBinType, binNum, maxPartitionSizePerBin,
            itemEncodedSlotSize, psLowDegree, queryPowers,
            plainModulus, polyModulusDegree, coeffModulusBits,
            expectServerSize, maxClientSize
        );
        if (Cmg21UpsiParamsChecker.checkValid(cmg21UpsiParams)) {
            return cmg21UpsiParams;
        } else {
            throw new IllegalArgumentException("Invalid SEAL parameters: " + cmg21UpsiParams);
        }
    }

    /**
     * serve size 2000, max client size 1.
     */
    public static final Cmg21UpsiParams SERVER_2K_CLIENT_MAX_1 = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NO_STASH_ONE_HASH, 512, 15,
        8,
        0, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
        40961, 4096, new int[]{24, 24, 24},
        2000, 1
    );

    /**
     * serve size 100000, max client size 1.
     */
    public static final Cmg21UpsiParams SERVER_100K_CLIENT_MAX_1 = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NO_STASH_ONE_HASH, 512, 20,
        8,
        0, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20},
        40961, 4096, new int[]{24, 24, 24},
        100000, 1
    );

    /**
     * serve size 1 million, max client size 1000, optimized to minimize computation cost.
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_MAX_1K_CMP = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 2046, 101,
        6,
        0, new int[]{1, 3, 4, 5, 8, 14, 20, 26, 32, 38, 44, 47, 48, 49, 51, 52},
        40961, 4096, new int[]{40, 32, 32},
        1000000, 1024
    );

    /**
     * serve size 1 million, max client size 1000, optimized to minimize communication cost.
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_MAX_1K_COM = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 1638, 125,
        5,
        5, new int[]{1, 2, 3, 4, 5, 6, 18, 30, 42, 54, 60},
        188417, 4096, new int[]{48, 36, 25},
        1000000, 1024
    );

    /**
     * serve size 1 million, max client size 11041.
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_MAX_11041 = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 16384, 98,
        4,
        8, new int[]{1, 3, 4, 9, 27},
        1785857, 8192, new int[]{56, 56, 24, 24},
        1000000, 11041
    );

    /**
     * serve size 1 million, max client size 2000, optimized to minimize computation cost.
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_MAX_2K_CMP = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 3410, 72,
        6,
        0, new int[]{1, 3, 4, 9, 11, 16, 20, 25, 27, 32, 33, 35, 36},
        40961, 4096, new int[]{40, 32, 32},
        1000000, 2048
    );

    /**
     * serve size 1 million, max client size 2000, optimized to minimize communication cost.
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_MAX_2K_COM = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 3410, 125,
        6,
        5, new int[]{1, 2, 3, 4, 5, 6, 18, 30, 42, 54, 60},
        65537, 4096, new int[]{48, 30, 30},
        1000000, 2048
    );

    /**
     * serve size 1 million, max client size 256.
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_MAX_256 = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 585, 180,
        7,
        0, new int[]{1, 3, 4, 6, 10, 13, 15, 21, 29, 37, 45, 53, 61, 69, 77, 81, 83, 86, 87, 90, 92, 96},
        40961, 4096, new int[]{40, 32, 32},
        1000000, 256
    );

    /**
     *  serve size 1 million, max client size 4000, optimized to minimize computation cost.
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_MAX_4K_CMP = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 6552, 40,
        5,
        0, new int[]{1, 3, 4, 9, 11, 16, 17, 19, 20},
        65537, 4096, new int[]{48, 30, 30},
        1000000, 4096
    );

    /**
     * serve size 1 million, max client size 4000, optimized to minimize communication cost.
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_MAX_4K_COM = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 6825, 98,
        6,
        8, new int[]{1, 3, 4, 9, 27},
        65537, 8192, new int[]{56, 56, 30},
        1000000, 4096
    );

    /**
     * serve size 1 million, max client size 512, optimized to minimize computation cost.
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_MAX_512_CMP = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 1364, 128,
        6,
        0, new int[]{1, 3, 4, 5, 8, 14, 20, 26, 32, 38, 44, 50, 56, 59, 60, 61, 63, 64},
        65537, 4096, new int[]{40, 34, 30},
        1000000, 512
    );

    /**
     * serve size 1 million, max client size 512, optimized to minimize communication cost.
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_MAX_512_COM = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 1364, 228,
        6,
        4, new int[]{1, 2, 3, 4, 5, 10, 15, 35, 55, 75, 95, 115, 125, 130, 140},
        65537, 4096, new int[]{48, 34, 27},
        1000000, 512
    );

    /**
     * serve size 1 million, max client size 5535.
     */
    public static final Cmg21UpsiParams SERVER_1M_CLIENT_MAX_5535 = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 8192, 98,
        4,
        8, new int[]{1, 3, 4, 9, 27},
        1785857, 8192, new int[]{56, 56, 24, 24},
        1000000, 5535
    );

    /**
     * serve size 16 million, max client size 1024.
     */
    public static final Cmg21UpsiParams SERVER_16M_CLIENT_MAX_1024 = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 1638, 1304,
        5,
        44, new int[]{1, 3, 11, 18, 45, 225},
        4079617, 8192, new int[]{56, 56, 56, 50},
        16000000, 1024
    );

    /**
     * serve size 16 million, max client size 2048.
     */
    public static final Cmg21UpsiParams SERVER_16M_CLIENT_MAX_2048 = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 3276, 1304,
        5,
        44, new int[]{1, 3, 11, 18, 45, 225},
        4079617, 8192, new int[]{56, 56, 56, 50},
        16000000, 2048
    );

    /**
     * serve size 16 million, max client size 4096.
     */
    public static final Cmg21UpsiParams SERVER_16M_CLIENT_MAX_4096 = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 6552, 1304,
        5,
        44, new int[]{1, 3, 11, 18, 45, 225},
        4079617, 8192, new int[]{56, 56, 56, 50},
        16000000, 4096
    );

    /**
     * serve size 16 million, max client size 11041.
     */
    public static final Cmg21UpsiParams SERVER_16M_CLIENT_MAX_11041 = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 16380, 1304,
        5,
        44, new int[]{1, 3, 11, 18, 45, 225},
        4079617, 8192, new int[]{56, 56, 56, 50},
        16000000, 11041
    );

    /**
     * serve size 256 million, max client size 1024.
     */
    public static final Cmg21UpsiParams SERVER_256M_CLIENT_MAX_1024 = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 2048, 4000,
        4,
        310, new int[]{ 1, 4, 10, 11, 28, 33, 78, 118, 143, 311, 1555},
        4079617, 8192, new int[]{50, 50, 50, 38, 30},
        256000000, 1024
    );

    /**
     * serve size 256 million, max client size 2048.
     */
    public static final Cmg21UpsiParams SERVER_256M_CLIENT_MAX_2048 = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 4096, 4000,
        4,
        310, new int[]{ 1, 4, 10, 11, 28, 33, 78, 118, 143, 311, 1555},
        4079617, 8192, new int[]{50, 50, 50, 38, 30},
        256000000, 2048
    );

    /**
     * serve size 256 million, max client size 4096.
     */
    public static final Cmg21UpsiParams SERVER_256M_CLIENT_MAX_4096 = Cmg21UpsiParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 6144, 4000,
        4,
        310, new int[]{ 1, 4, 10, 11, 28, 33, 78, 118, 143, 311, 1555},
        4079617, 8192, new int[]{50, 50, 50, 38, 30},
        256000000, 4096
    );

    /**
     * return cuckoo hash type.
     *
     * @return cuckoo hash type.
     */
    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    /**
     * return hash num.
     *
     * @return hash num.
     */
    public int getCuckooHashNum() {
        return CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    /**
     * return bin num.
     *
     * @return bin num.
     */
    public int getBinNum() {
        return binNum;
    }

    /**
     * return max partition size per bin.
     *
     * @return max partition size per bin.
     */
    public int getMaxPartitionSizePerBin() {
        return maxPartitionSizePerBin;
    }

    /**
     * return item encoded slot size.
     *
     * @return item encoded slot size.
     */
    public int getItemEncodedSlotSize() {
        return itemEncodedSlotSize;
    }

    /**
     * return Paterson-Stockmeyer low degree.
     *
     * @return Paterson-Stockmeyer low degree.
     */
    public int getPsLowDegree() {
        return psLowDegree;
    }

    /**
     * return query powers.
     *
     * @return query powers.
     */
    public int[] getQueryPowers() {
        return queryPowers;
    }

    /**
     * return plain modulus.
     *
     * @return plain modulus.
     */
    public long getPlainModulus() {
        return plainModulus;
    }

    /**
     * return poly modulus degree.
     *
     * @return poly modulus degree.
     */
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    /**
     * return coeff modulus bits.
     *
     * @return coeff modulus bits.
     */
    public int[] getCoeffModulusBits() {
        return coeffModulusBits;
    }

    /**
     * return ciphertext num.
     *
     * @return ciphertext num.
     */
    public int getCiphertextNum() {
        return ciphertextNum;
    }

    /**
     * return item per ciphertext.
     *
     * @return item per ciphertext.
     */
    public int getItemPerCiphertext() {
        return itemPerCiphertext;
    }

    @Override
    public String toString() {
        return "Parameters chosen:" + "\n" +
            "  - hash_bin_params: {" + "\n" +
            "     - cuckoo_hash_bin_type : " + cuckooHashBinType + "\n" +
            "     - bin_num : " + binNum + "\n" +
            "     - max_items_per_bin : " + maxPartitionSizePerBin + "\n" +
            "  }" + "\n" +
            "  - item_params: {" + "\n" +
            "     - felts_per_item : " + itemEncodedSlotSize + "\n" +
            "  }" + "\n" +
            "  - query_params: {" + "\n" +
            "     - ps_low_degree : " + psLowDegree + "\n" +
            "     - query_powers : " + Arrays.toString(queryPowers) + "\n" +
            "  }" + "\n" +
            "  - seal_params: {" + "\n" +
            "     - plain_modulus : " + plainModulus + "\n" +
            "     - poly_modulus_degree : " + polyModulusDegree + "\n" +
            "     - coeff_modulus_bits : " + Arrays.toString(coeffModulusBits) + "\n" +
            "  }" + "\n";
    }

    @Override
    public int maxClientElementSize() {
        return maxClientSize;
    }

    @Override
    public int expectServerSize() {
        return expectServerSize;
    }
}

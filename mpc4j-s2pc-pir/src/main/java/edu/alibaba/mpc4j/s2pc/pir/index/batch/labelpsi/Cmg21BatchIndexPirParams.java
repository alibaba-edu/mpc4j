package edu.alibaba.mpc4j.s2pc.pir.index.batch.labelpsi;

import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * CMG21 Batch Index PIR params.
 *
 * @author Liqiang Peng
 * @date 2024/2/19
 */
public class Cmg21BatchIndexPirParams {
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

    private Cmg21BatchIndexPirParams(CuckooHashBinType cuckooHashBinType, int binNum,
                                     int maxPartitionSizePerBin, int itemEncodedSlotSize, int psLowDegree,
                                     int[] queryPowers, long plainModulus, int polyModulusDegree,
                                     int[] coeffModulusBits, int maxClientSize) {
        this.cuckooHashBinType = cuckooHashBinType;
        this.binNum = binNum;
        this.maxPartitionSizePerBin = maxPartitionSizePerBin;
        this.itemEncodedSlotSize = itemEncodedSlotSize;
        this.psLowDegree = psLowDegree;
        this.queryPowers = queryPowers;
        this.plainModulus = plainModulus;
        this.polyModulusDegree = polyModulusDegree;
        this.coeffModulusBits = coeffModulusBits;
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
     * @param maxClientSize          max client size.
     * @return UPSI params.
     */
    public static Cmg21BatchIndexPirParams create(CuckooHashBinType cuckooHashBinType, int binNum,
                                                  int maxPartitionSizePerBin, int itemEncodedSlotSize, int psLowDegree,
                                                  int[] queryPowers, long plainModulus, int polyModulusDegree,
                                                  int[] coeffModulusBits, int maxClientSize) {
        return new Cmg21BatchIndexPirParams(
            cuckooHashBinType, binNum, maxPartitionSizePerBin,
            itemEncodedSlotSize, psLowDegree, queryPowers,
            plainModulus, polyModulusDegree, coeffModulusBits,
            maxClientSize
        );
    }

    /**
     * max client log size 12, expect server log size 24.
     */
    public static final Cmg21BatchIndexPirParams SERVER_LOG_SIZE_24_CLIENT_LOG_SIZE_12 = new Cmg21BatchIndexPirParams(
        CuckooHashBinType.NAIVE_3_HASH, 1 << 13, 4000, 1,
        310, new int[]{ 1, 4, 10, 11, 28, 33, 78, 118, 143, 311, 1555},
        33538049, 8192, new int[]{50, 50, 50, 38, 30},
        1 << 12
    );

    /**
     * max client log size 12, expect server log size 22.
     */
    public static final Cmg21BatchIndexPirParams SERVER_LOG_SIZE_22_CLIENT_LOG_SIZE_12 = new Cmg21BatchIndexPirParams(
        CuckooHashBinType.NAIVE_3_HASH, 1 << 13, 1304, 1,
        44, new int[]{1, 3, 11, 18, 45, 225},
        33538049, 8192, new int[]{50, 50, 50, 38, 30},
        1 << 12
    );

    /**
     * max client log size 12, expect server log size 20.
     */
    public static final Cmg21BatchIndexPirParams SERVER_LOG_SIZE_20_CLIENT_LOG_SIZE_12 = new Cmg21BatchIndexPirParams(
        CuckooHashBinType.NAIVE_3_HASH, 1 << 13, 228, 1,
        44, new int[]{1, 3, 11, 18, 45},
        33538049, 8192, new int[]{50, 50, 50, 38, 30},
        1 << 12
    );

    /**
     * max client log size 10, expect server log size 16.
     */
    public static final Cmg21BatchIndexPirParams SERVER_LOG_SIZE_16_CLIENT_LOG_SIZE_10 = new Cmg21BatchIndexPirParams(
        CuckooHashBinType.NAIVE_3_HASH, 1 << 12, 80, 1,
        0, new int[]{1, 3, 4, 9, 11, 16, 20, 25, 27, 32, 33, 35, 36, 37, 39, 40},
        65537, 4096, new int[]{48, 30, 30},
        1 << 10
    );

    /**
     * max client log size 12, expect server log size 16.
     */
    public static final Cmg21BatchIndexPirParams SERVER_LOG_SIZE_16_CLIENT_LOG_SIZE_12 = new Cmg21BatchIndexPirParams(
        CuckooHashBinType.NAIVE_3_HASH, 1 << 13, 53, 1,
        8, new int[]{1, 3, 4, 9, 27},
        65537, 8192, new int[]{56, 56, 30},
        1 << 12
    );

    /**
     * max client log size 10, expect server log size 12.
     */
    public static final Cmg21BatchIndexPirParams SERVER_LOG_SIZE_12_CLIENT_LOG_SIZE_10 = new Cmg21BatchIndexPirParams(
        CuckooHashBinType.NAIVE_3_HASH, 1 << 12, 20, 1,
        0, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20},
        65537, 4096, new int[]{48, 30, 30},
        1 << 10
    );

    /**
     * max client log size 12, expect server log size 12.
     */
    public static final Cmg21BatchIndexPirParams SERVER_LOG_SIZE_12_CLIENT_LOG_SIZE_12 = new Cmg21BatchIndexPirParams(
        CuckooHashBinType.NAIVE_3_HASH, 1 << 13, 26, 1,
        8, new int[]{1, 3, 4, 9},
        65537, 8192, new int[]{56, 56, 30},
        1 << 12
    );

    public Cmg21BatchIndexPirParams copy() {
        return new Cmg21BatchIndexPirParams(
            this.cuckooHashBinType,
            this.binNum,
            this.maxPartitionSizePerBin,
            this.itemEncodedSlotSize,
            this.psLowDegree,
            this.queryPowers.clone(),
            this.plainModulus,
            this.polyModulusDegree,
            this.coeffModulusBits.clone(),
            this.maxClientSize
        );
    }

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

    public int maxClientElementSize() {
        return maxClientSize;
    }

}

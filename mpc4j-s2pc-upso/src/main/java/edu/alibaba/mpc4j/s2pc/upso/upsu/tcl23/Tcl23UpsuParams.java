package edu.alibaba.mpc4j.s2pc.upso.upsu.tcl23;

import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.upso.upsu.UpsuParams;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * TCL23 UPSU params.
 *
 * @author Liqiang Peng
 * @date 2024/3/7
 */
public class Tcl23UpsuParams implements UpsuParams {
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
     * max sender size
     */
    private final int maxSenderSize;
    /**
     * item per ciphertext
     */
    private final int itemPerCiphertext;
    /**
     * ciphertext num
     */
    private final int ciphertextNum;
    /**
     * encryption parameters
     */
    private final byte[] encryptionParameters;
    /**
     * plain modulus size
     */
    private final int plainModulusSize;
    /**
     * bit length per encoded item
     */
    private final int l;

    private Tcl23UpsuParams(CuckooHashBinType cuckooHashBinType, int binNum, int maxPartitionSizePerBin,
                            int itemEncodedSlotSize, int psLowDegree, int[] queryPowers,
                            long plainModulus, int polyModulusDegree, int[] coeffModulusBits, int maxSenderSize) {
        this.cuckooHashBinType = cuckooHashBinType;
        this.binNum = binNum;
        this.maxPartitionSizePerBin = maxPartitionSizePerBin;
        this.itemEncodedSlotSize = itemEncodedSlotSize;
        this.psLowDegree = psLowDegree;
        this.queryPowers = queryPowers;
        this.plainModulus = plainModulus;
        this.polyModulusDegree = polyModulusDegree;
        this.coeffModulusBits = coeffModulusBits;
        this.maxSenderSize = maxSenderSize;
        this.itemPerCiphertext = polyModulusDegree / itemEncodedSlotSize;
        this.ciphertextNum = binNum / itemPerCiphertext;
        this.encryptionParameters = Tcl23UpsuNativeUtils.genEncryptionParameters(
            polyModulusDegree, plainModulus, coeffModulusBits
        );
        this.plainModulusSize = BigInteger.valueOf(plainModulus).bitLength();
        this.l = itemEncodedSlotSize * plainModulusSize;
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
     * @param maxSenderSize          max sender size.
     * @return UPSU params.
     */
    public static Tcl23UpsuParams create(CuckooHashBinType cuckooHashBinType, int binNum, int maxPartitionSizePerBin,
                                         int itemEncodedSlotSize, int psLowDegree, int[] queryPowers,
                                         long plainModulus, int polyModulusDegree, int[] coeffModulusBits, int maxSenderSize) {
        Tcl23UpsuParams tcl23UpsuParams = new Tcl23UpsuParams(
            cuckooHashBinType, binNum, maxPartitionSizePerBin, itemEncodedSlotSize,
            psLowDegree, queryPowers, plainModulus, polyModulusDegree, coeffModulusBits, maxSenderSize
        );
        if (Tcl23UpsuParamsChecker.checkValid(tcl23UpsuParams)) {
            return tcl23UpsuParams;
        } else {
            throw new IllegalArgumentException("Invalid UPSU parameters: " + tcl23UpsuParams);
        }
    }

    /**
     * expect receiver size 16 million, max sender size 1024.
     */
    public static final Tcl23UpsuParams RECEIVER_16M_SENDER_MAX_1024 = Tcl23UpsuParams.create(
        CuckooHashBinType.NAIVE_3_HASH, 1638, 1304,
        5,
        44, new int[]{1, 3, 11, 18, 45, 225},
        4079617, 8192, new int[]{56, 56, 56, 50},
        1024
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

    /**
     * return bit length per encoded item.
     *
     * @return bit length per encoded item.
     */
    public int getL() {
        return l;
    }

    /**
     * return plain modulus size.
     *
     * @return plain modulus size.
     */
    public int getPlainModulusSize() {
        return plainModulusSize;
    }

    /**
     * return encryption parameters.
     *
     * @return encryption parameters.
     */
    public byte[] getEncryptionParameters() {
        return encryptionParameters;
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
    public int maxSenderElementSize() {
        return maxSenderSize;
    }
}

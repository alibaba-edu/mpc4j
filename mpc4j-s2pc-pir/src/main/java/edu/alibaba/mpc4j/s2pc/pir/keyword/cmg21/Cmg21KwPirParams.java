package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * CMG21 keyword PIR params.
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class Cmg21KwPirParams implements KwPirParams {
    /**
     * cuckoo hash bin type
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
     * coeffs modulus bits
     */
    private final int[] coeffModulusBits;
    /**
     * expect server size
     */
    private final int expectServerSize;
    /**
     * max retrieval size
     */
    private final int maxRetrievalSize;
    /**
     * encryption params
     */
    private final byte[] encryptionParams;
    /**
     * item per ciphertext
     */
    private final int itemPerCiphertext;
    /**
     * ciphertext num
     */
    private final int ciphertextNum;

    private Cmg21KwPirParams(CuckooHashBinType cuckooHashBinType, int binNum, int maxPartitionSizePerBin,
                             int itemEncodedSlotSize, int psLowDegree, int[] queryPowers,
                             long plainModulus, int polyModulusDegree, int[] coeffModulusBits,
                             int expectServerSize, int maxRetrievalSize) {
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
        this.maxRetrievalSize = maxRetrievalSize;
        this.itemPerCiphertext = polyModulusDegree / itemEncodedSlotSize;
        this.ciphertextNum = binNum / itemPerCiphertext;
        this.encryptionParams = Cmg21KwPirNativeUtils.genEncryptionParameters(polyModulusDegree, plainModulus, coeffModulusBits);
    }

    /**
     * create keyword PIR params without checking the validity.
     *
     * @param cuckooHashBinType      cuckoo hash bin type.
     * @param binNum                 bin num.
     * @param maxPartitionSizePerBin max partition size per bin.
     * @param itemEncodedSlotSize    item encoded slot size.
     * @param psLowDegree            Paterson-Stockmeyer low degree.
     * @param queryPowers            query powers.
     * @param plainModulus           plain modulus.
     * @param polyModulusDegree      poly modulus degree.
     * @param coeffModulusBits       coeffs modulus bits.
     * @param expectServerSize       expect server size.
     * @param maxRetrievalSize       max retrieval size.
     * @return keyword PIR params.
     */
    public static Cmg21KwPirParams uncheckCreate(CuckooHashBinType cuckooHashBinType, int binNum, int maxPartitionSizePerBin,
                                                 int itemEncodedSlotSize, int psLowDegree, int[] queryPowers,
                                                 long plainModulus, int polyModulusDegree, int[] coeffModulusBits,
                                                 int expectServerSize, int maxRetrievalSize) {
        return new Cmg21KwPirParams(
            cuckooHashBinType, binNum, maxPartitionSizePerBin,
            itemEncodedSlotSize, psLowDegree, queryPowers,
            plainModulus, polyModulusDegree, coeffModulusBits,
            expectServerSize, maxRetrievalSize);
    }

    /**
     * create a valid keyword PIR params.
     *
     * @param cuckooHashBinType      cuckoo hash bin type.
     * @param binNum                 bin num.
     * @param maxPartitionSizePerBin max partition size per bin.
     * @param itemEncodedSlotSize    item encoded slot size.
     * @param psLowDegree            Paterson-Stockmeyer low degree.
     * @param queryPowers            query powers.
     * @param plainModulus           plain modulus.
     * @param polyModulusDegree      poly modulus degree.
     * @param coeffModulusBits       coeffs modulus bits.
     * @param expectServerSize       expect server size.
     * @param maxRetrievalSize       max retrieval size.
     * @return keyword PIR params.
     */
    public static Cmg21KwPirParams create(CuckooHashBinType cuckooHashBinType, int binNum,
                                          int maxPartitionSizePerBin, int itemEncodedSlotSize, int psLowDegree,
                                          int[] queryPowers, long plainModulus, int polyModulusDegree,
                                          int[] coeffModulusBits, int expectServerSize, int maxRetrievalSize) {
        Cmg21KwPirParams cmg21KwPirParams = uncheckCreate(
            cuckooHashBinType, binNum, maxPartitionSizePerBin,
            itemEncodedSlotSize, psLowDegree, queryPowers,
            plainModulus, polyModulusDegree, coeffModulusBits,
            expectServerSize, maxRetrievalSize);
        if (Cmg21KwPirParamsChecker.checkValid(cmg21KwPirParams)) {
            return cmg21KwPirParams;
        } else {
            throw new IllegalArgumentException("Invalid SEAL parameters: " + cmg21KwPirParams);
        }
    }

    /**
     * expect server size 1 million, max client retrieval size 4096
     */
    public static final Cmg21KwPirParams SERVER_1M_CLIENT_MAX_4096 = Cmg21KwPirParams.uncheckCreate(
        CuckooHashBinType.NAIVE_3_HASH, 6552, 770,
        5,
        26, new int[]{1, 5, 8, 27, 135},
        1785857L, 8192, new int[]{50, 56, 56, 50},
        1000000, 4096
    );

    /**
     * expect server size 1 million, max client retrieval size 1
     */
    public static final Cmg21KwPirParams SERVER_1M_CLIENT_MAX_1 = Cmg21KwPirParams.uncheckCreate(
        CuckooHashBinType.NO_STASH_ONE_HASH, 1638, 228,
        5,
        0, new int[]{1, 3, 8, 19, 33, 39, 92, 102},
        65537L, 8192, new int[]{56, 48, 48},
        1000000, 1
    );

    /**
     * expect server size 100K, max client retrieval size 1
     */
    public static final Cmg21KwPirParams SERVER_100K_CLIENT_MAX_1 = Cmg21KwPirParams.uncheckCreate(
        CuckooHashBinType.NO_STASH_ONE_HASH, 409, 42,
        5,
        0, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42},
        65537L, 2048, new int[]{48},
        100000, 1
    );

    /**
     * expect server size 16M, max client retrieval size 1
     */
    public static final Cmg21KwPirParams SERVER_16M_CLIENT_MAX_1 = Cmg21KwPirParams.uncheckCreate(
        CuckooHashBinType.NO_STASH_ONE_HASH, 2048, 782,
        4,
        26, new int[]{1, 5, 8, 27, 135},
        1785857L, 8192, new int[]{56, 56, 56, 50},
        16000000, 1
    );

    /**
     * return cuckoo hash bin type.
     *
     * @return cuckoo hash bin type.
     */
    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    /**
     * return cuckoo hash key num.
     *
     * @return cuckoo hash key num.
     */
    public int getCuckooHashKeyNum() {
        return CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    /**
     * return cuckoo hash bin num.
     *
     * @return cuckoo hash bin num.
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
     * return coeffs modulus bit.
     *
     * @return coeffs modulus bit.
     */
    public int[] getCoeffModulusBits() {
        return coeffModulusBits;
    }

    @Override
    public int maxRetrievalSize() {
        return maxRetrievalSize;
    }

    public int expectServerSize() {
        return expectServerSize;
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

    /**
     * return hash bin entry encoded array.
     *
     * @param hashBinEntry hash bin entry.
     * @param isReceiver   is receiver.
     * @param secureRandom secure random.
     * @return hash bin entry encoded array.
     */
    public long[] getHashBinEntryEncodedArray(HashBinEntry<ByteBuffer> hashBinEntry, boolean isReceiver,
                                              SecureRandom secureRandom) {
        long[] encodedArray = new long[itemEncodedSlotSize];
        int bitLength = (BigInteger.valueOf(plainModulus).bitLength() - 1) * itemEncodedSlotSize;
        assert bitLength >= 80;
        int shiftBits = BigInteger.valueOf(plainModulus).bitLength() - 1;
        BigInteger shiftMask = BigInteger.ONE.shiftLeft(shiftBits).subtract(BigInteger.ONE);
        if (hashBinEntry.getHashIndex() != -1) {
            assert (hashBinEntry.getHashIndex() < 3) : "hash index should be [0, 1, 2]";
            BigInteger input = BigIntegerUtils.byteArrayToNonNegBigInteger(hashBinEntry.getItem().array());
            input = input.mod(BigInteger.ONE.shiftLeft(CommonConstants.BLOCK_BIT_LENGTH));
            for (int i = 0; i < itemEncodedSlotSize; i++) {
                encodedArray[i] = input.and(shiftMask).longValueExact();
                input = input.shiftRight(shiftBits);
            }
        } else {
            IntStream.range(0, itemEncodedSlotSize).forEach(i -> {
                long random = Math.abs(secureRandom.nextLong()) % plainModulus / 4;
                encodedArray[i] = random << 1 | (isReceiver ? 1L : 0L);
            });
        }
        for (int i = 0; i < itemEncodedSlotSize; i++) {
            assert encodedArray[i] < plainModulus;
        }
        return encodedArray;
    }

    /**
     * encode label.
     *
     * @param labelBytes   label.
     * @param partitionNum partition num.
     * @return encoded label.
     */
    public long[][] encodeLabel(byte[] labelBytes, int partitionNum) {
        long[][] encodedArray = new long[partitionNum][itemEncodedSlotSize];
        int shiftBits = CommonUtils.getUnitNum(labelBytes.length * Byte.SIZE, itemEncodedSlotSize * partitionNum);
        BigInteger bigIntLabel = BigIntegerUtils.byteArrayToNonNegBigInteger(labelBytes);
        BigInteger shiftMask = BigInteger.ONE.shiftLeft(shiftBits).subtract(BigInteger.ONE);
        for (int i = 0; i < partitionNum; i++) {
            for (int j = 0; j < itemEncodedSlotSize; j++) {
                encodedArray[i][j] = bigIntLabel.and(shiftMask).longValueExact();
                bigIntLabel = bigIntLabel.shiftRight(shiftBits);
            }
        }
        for (int i = 0; i < partitionNum; i++) {
            for (int j = 0; j < itemEncodedSlotSize; j++) {
                assert (encodedArray[i][j] < plainModulus);
            }
        }
        return encodedArray;
    }

    public byte[] getEncryptionParams() {
        return encryptionParams;
    }

    public int getItemPerCiphertext() {
        return itemPerCiphertext;
    }

    public int getCiphertextNum() {
        return ciphertextNum;
    }
}
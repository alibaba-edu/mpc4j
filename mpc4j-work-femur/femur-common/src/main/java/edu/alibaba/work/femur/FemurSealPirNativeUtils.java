package edu.alibaba.work.femur;

import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Femur SEAL PIR native utils.
 *
 * @author Liqiang Peng
 * @date 2024/9/19
 */
public class FemurSealPirNativeUtils {

    private FemurSealPirNativeUtils() {
        // empty
    }

    /**
     * generate encryption params.
     *
     * @param polyModulusDegree poly modulus degree.
     * @param plainModulus      plain modulus.
     * @return encryption params.
     */
    public static native byte[] generateEncryptionParams(int polyModulusDegree, long plainModulus);

    /**
     * generate key pair.
     *
     * @param encryptionParams encryption params.
     * @return key pair.
     */
    public static native List<byte[]> keyGen(byte[] encryptionParams);

    /**
     * NTT transformation.
     *
     * @param encryptionParams encryption params.
     * @param plaintextList    plaintext list.
     * @return BFV plaintexts in NTT form.
     */
    public static native List<byte[]> transformToNtt(byte[] encryptionParams, List<long[]> plaintextList);

    /**
     * INTT transformation.
     *
     * @param encryptionParams encryption params.
     * @param plaintextList    plaintext list.
     * @return BFV plaintexts in coefficient form.
     */
    public static native long[][] transformFromNtt(byte[] encryptionParams, byte[][] plaintextList);

    /**
     * generate query.
     *
     * @param encryptionParams encryption params.
     * @param publicKey        public key.
     * @param secretKey        secret key.
     * @param indices          indices.
     * @param nvec             dimension size.
     * @return query ciphertexts.
     */
    public static native List<byte[]> generateQuery(byte[] encryptionParams, byte[] publicKey, byte[] secretKey,
                                                    int[] indices, int[] nvec);

    /**
     * generate response.
     *
     * @param encryptionParams encryption params.
     * @param galoisKey        Galois keys.
     * @param queryList        query ciphertexts.
     * @param database         database.
     * @param nvec             dimension size.
     * @return response ciphertexts。
     */
    public static native List<byte[]> generateReply(byte[] encryptionParams, byte[] galoisKey, List<byte[]> queryList,
                                                    byte[][] database, int[] nvec);

    /**
     * decode response.
     *
     * @param encryptionParams encryption params.
     * @param secretKey        secret key.
     * @param response         response ciphertext.
     * @param dimension        dimension.
     * @return BFV plaintext.
     */
    public static native long[] decryptReply(byte[] encryptionParams, byte[] secretKey, List<byte[]> response, int dimension);

    /**
     * compute size ratio between a ciphertext and the largest plaintext that can be encrypted.
     *
     * @param encryptionParams encryption params.
     * @return expansion ratio.
     */
    public static native int expansionRatio(byte[] encryptionParams);

    public static long[] convertBytesToCoeffs(int size, int limit, byte[] byteArray) {
        int longArraySize = CommonUtils.getUnitNum(Byte.SIZE * size, limit);
        long[] longArray = new long[longArraySize];
        int room = limit;
        int flag = 0;
        for (int i = 0; i < size; i++) {
            int src = byteArray[i];
            if (src < 0) {
                src &= 0xFF;
            }
            int rest = Byte.SIZE;
            while (rest != 0) {
                if (room == 0) {
                    flag++;
                    room = limit;
                }
                int shift = Math.min(room, rest);
                long temp = longArray[flag] << shift;
                longArray[flag] = temp | (src >> (Byte.SIZE - shift));
                int remain = (1 << (Byte.SIZE - shift)) - 1;
                src = (src & remain) << shift;
                room -= shift;
                rest -= shift;
            }
        }
        longArray[flag] = longArray[flag] << room;
        return longArray;
    }

    public static byte[] convertCoeffsToBytes(long[] coeffArray, int logt) {
        int len = CommonUtils.getUnitNum(coeffArray.length * logt, Byte.SIZE);
        byte[] byteArray = new byte[len];
        int room = Byte.SIZE;
        int j = 0;
        for (long l : coeffArray) {
            long src = l;
            int rest = logt;
            while (rest != 0 && j < byteArray.length) {
                int shift = Math.min(room, rest);
                byteArray[j] = (byte) (byteArray[j] << shift);
                byteArray[j] = (byte) (byteArray[j] | (src >> (logt - shift)));
                src = src << shift;
                room -= shift;
                rest -= shift;
                if (room == 0) {
                    j++;
                    room = Byte.SIZE;
                }
            }
        }
        return byteArray;
    }

    public static int[] computeDimensionLength(int elementSize, int dimension) {
        int[] dimensionLength = IntStream.range(0, dimension)
            .map(i -> (int) Math.max(2, Math.floor(Math.pow(elementSize, 1.0 / dimension))))
            .toArray();
        int product = 1;
        int j = 0;
        // if plaintext_num is not a d-power
        if (dimensionLength[0] != Math.pow(elementSize, 1.0 / dimension)) {
            while (product < elementSize && j < dimension) {
                product = 1;
                dimensionLength[j++]++;
                for (int i = 0; i < dimension; i++) {
                    product *= dimensionLength[i];
                }
            }
        }
        return dimensionLength;
    }

    public static int[] decomposeIndex(int x, int[] dimensionSize) {
        long longProduct = Arrays.stream(dimensionSize).asLongStream().reduce(1, (di, dj) -> di * dj);
        // since database size must be an integer, we have that d_1 * ... d_t <= n
        assert longProduct <= Integer.MAX_VALUE;
        int product = (int) longProduct;
        int[] indices = new int[dimensionSize.length];
        for (int i = 0; i < dimensionSize.length; i++) {
            product /= dimensionSize[i];
            int xi = x / product;
            indices[i] = xi;
            x -= xi * product;
        }
        return indices;
    }
}

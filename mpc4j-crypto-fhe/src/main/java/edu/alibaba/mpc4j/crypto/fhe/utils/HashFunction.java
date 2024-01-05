package edu.alibaba.mpc4j.crypto.fhe.utils;

import edu.alibaba.mpc4j.crypto.fhe.zq.Common;
import org.bouncycastle.crypto.digests.Blake2bDigest;

/**
 * HashFunction used to calculate the id of the EncryptionParams object.
 * <p></p>
 * The implementation is from: https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/hash.h
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/30
 */
public class HashFunction {
    /**
     * private constructor.
     */
    private HashFunction() {
        // empty
    }
    /**
     * 32-byte, 4 * 64-bit
     */
    public final static int HASH_BLOCK_UINT64_COUNT = 4;
    /**
     * hash of zero block
     */
    public final static long[] HASH_ZERO_BLOCK = new long[]{0, 0, 0, 0};
    /**
     * Black2B hash
     */
    private final static Blake2bDigest BLAKE_2B = new Blake2bDigest(HASH_BLOCK_UINT64_COUNT * 64);

    /**
     * Computes the hash of the input.
     *
     * @param input       the input data.
     * @param uint64Count length of the data in uint64 size.
     * @param destination place to set the output.
     */
    public static void hash(long[] input, int uint64Count, long[] destination) {
        // convert input to bytes and hash
        byte[] inputBytes = Common.uint64ArrayToByteArray(input, uint64Count);
        BLAKE_2B.update(inputBytes, 0, inputBytes.length);
        byte[] hash = new byte[BLAKE_2B.getDigestSize()];
        BLAKE_2B.doFinal(hash, 0);
        // convert back to long
        long[] temp = Common.byteArrayToUint64Array(hash, hash.length);
        System.arraycopy(temp, 0, destination, 0, HASH_BLOCK_UINT64_COUNT);
    }
}

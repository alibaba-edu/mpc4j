package edu.alibaba.mpc4j.crypto.fhe.seal.utils;

import edu.alibaba.mpc4j.crypto.fhe.seal.rand.primitive.Blake2b;

/**
 * HashFunction used to calculate the id of the EncryptionParams object.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/hash.h">hash.h</a>,
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
     * Computes the hash of the input.
     *
     * @param input       the input data.
     * @param uint64Count length of the data in uint64 size.
     * @param destination place to set the output.
     */
    public static void hash(long[] input, int uint64Count, long[] destination) {
        // See https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/hash.h#L32C21-L32C117
        // blake2b(&destination, hash_block_byte_count, input, uint64_count * bytes_per_uint64, nullptr, 0);
        Blake2b.blake2b(destination, input, uint64Count);
    }
}

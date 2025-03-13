/*
 * BLAKE2 reference source code package - reference C implementations
 *
 * Copyright 2012, Samuel Neves <sneves@dei.uc.pt>.  You may use this under the
 * terms of the CC0, the OpenSSL Licence, or the Apache Public License 2.0, at
 * your option.  The terms of these licenses can be found at:
 *
 * - CC0 1.0 Universal : http://creativecommons.org/publicdomain/zero/1.0
 * - OpenSSL license   : https://www.openssl.org/source/license.html
 * - Apache 2.0        : http://www.apache.org/licenses/LICENSE-2.0
 *
 *  More information about the BLAKE2 hash function can be found at
 *  https://blake2.net.
 */
package edu.alibaba.mpc4j.crypto.fhe.seal.rand.primitive;

import org.bouncycastle.util.Pack;

import java.util.Arrays;

/**
 * Blake2 parameters, states, and utility functions that are used in Blake2b and Blake2xb.
 * <p>
 * The implementations are based on
 * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2.h">blake2.h</a>
 * and
 * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2-impl.h">blake2-impl.h</a>,
 * with some functions from <a href="https://github.com/0xShamil/blake2b">blake2b</a>.
 * <p>
 * We tried several pure-Java blake2b implementations, including
 * <a href="https://github.com/bcgit/bc-java/blob/main/core/src/main/java/org/bouncycastle/crypto/digests/Blake2bDigest.java">
 * Bouncy Castle
 * </a>, <a href="https://github.com/0xShamil/blake2b">blake2b</a>, and <a href="https://github.com/alphazero/Blake2b">Blake2b</a>.
 * However, no implementation can provide the same output under the same parameters used in SEAL. Therefore, we have to
 * manually implement Blake2b and Blake2xb by ourselves.
 *
 * @author Weiran Liu
 * @date 2025/2/11
 */
public class Blake2 {
    /**
     * block bytes,
     * defined in <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2.h#L50">blake2.h</a>.
     */
    static final int BLAKE2B_BLOCK_BYTES = 128;
    /**
     * out bytes,
     * defined in <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2.h#L51">blake2.h</a>.
     */
    static final int BLAKE2B_OUT_BYTES = 64;
    /**
     * key bytes,
     * defined in <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2.h#L52">blake2.h</a>.
     */
    static final int BLAKE2B_KEY_BYTES = 64;
    /**
     * salt bytes,
     * defined in <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2.h#L53">blake2.h</a>.
     */
    static final int BLAKE2B_SALT_BYTES = 16;
    /**
     * personal bytes,
     * defined in <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2.h#L54">blake2.h</a>.
     */
    static final int BLAKE2B_PERSONAL_BYTES = 16;

    /**
     * Blake2b state,
     * defined in <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2.h#L68">blake2.h</a>.
     */
    static class Blake2bState {
        /**
         * uint64_t h[8]
         */
        long[] h = new long[8];
        /**
         * uint64_t t[2]
         */
        long[] t = new long[2];
        /**
         * uint64_t f[2]
         */
        long[] f = new long[2];
        /**
         * uint8_t buf[BLAKE2B_BLOCK_BYTES]
         */
        byte[] buf = new byte[BLAKE2B_BLOCK_BYTES];
        /**
         * size_t buflen
         */
        int buf_len;
        /**
         * size_t outlen
         */
        int out_len;
        /**
         * last_node
         */
        byte last_node;

        /**
         * Clears the state using memset in C/C++, i.e., memset(S, 0, sizeof(blake2b_state)).
         */
        void set_empty_state() {
            Arrays.fill(h, 0L);
            Arrays.fill(t, 0L);
            Arrays.fill(f, 0L);
            Arrays.fill(buf, (byte) 0);
            buf_len = 0;
            out_len = 0;
            last_node = 0;
        }
    }

    /**
     * Blake2b parameters,
     * defined in <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2.h#L114">blake2.h</a>.
     */
    static class Blake2bParam {
        /**
         * parameter byte length
         */
        static final int BLAKE2B_PARAM_BYTES = 64;
        /**
         * param in bytes
         */
        final byte[] param = new byte[BLAKE2B_PARAM_BYTES];

        void set_empty_param() {
            Arrays.fill(param, (byte) 0);
        }

        /**
         * Sets digest length.
         *
         * @param digest_length digest length.
         */
        void set_digest_length(int digest_length) {
            param[0] = (byte) digest_length;
        }

        int get_digest_length() {
            return param[0] & 0xFF;
        }

        void set_key_length(int key_length) {
            param[1] = (byte) key_length;
        }

        int get_key_length() {
            return param[1] & 0xFF;
        }

        void set_fanout(int fanout) {
            param[2] = (byte) fanout;
        }

        int get_fanout() {
            return param[2] & 0xFF;
        }

        void set_depth(int depth) {
            param[3] = (byte) depth;
        }

        int get_depth() {
            return param[3] & 0xFF;
        }

        void set_leaf_length(int leaf_length) {
            store32(param, 4, leaf_length);
        }

        int get_leaf_length() {
            return load32(param, 4);
        }

        void set_node_offset(int node_offset) {
            store32(param, 8, node_offset);
        }

        int get_node_offset() {
            return load32(param, 8);
        }

        void set_xof_length(int xor_length) {
            store32(param, 12, xor_length);
        }

        int get_xof_length() {
            return load32(param, 12);
        }

        void set_node_depth(int node_depth) {
            param[16] = (byte) node_depth;
        }

        int get_node_depth() {
            return param[16] & 0xFF;
        }

        void set_inner_length(int inner_length) {
            param[17] = (byte) inner_length;
        }

        int get_inner_length() {
            return param[17] & 0xFF;
        }

        void set_empty_reserved() {
            Arrays.fill(param, 18, 18 + 14, (byte) 0);
        }

        void set_reserved(byte[] reserved) {
            assert reserved.length == 14;
            System.arraycopy(reserved, 0, param, 18, reserved.length);
        }

        byte[] get_reserved() {
            byte[] reserved = new byte[14];
            System.arraycopy(param, 18, reserved, 0, reserved.length);
            return reserved;
        }

        void set_empty_salt() {
            Arrays.fill(param, 32, 32 + BLAKE2B_SALT_BYTES, (byte) 0);
        }

        void set_salt(byte[] salt) {
            assert salt.length == BLAKE2B_SALT_BYTES;
            System.arraycopy(salt, 0, param, 32, salt.length);
        }

        byte[] get_salt() {
            byte[] salt = new byte[BLAKE2B_SALT_BYTES];
            System.arraycopy(param, 32, salt, 0, salt.length);
            return salt;
        }

        void set_empty_personal() {
            Arrays.fill(param, 48, 48 + BLAKE2B_PERSONAL_BYTES, (byte) 0);
        }

        void set_personal(byte[] personal) {
            assert personal.length == BLAKE2B_PERSONAL_BYTES;
            System.arraycopy(personal, 0, param, 48, personal.length);
        }

        byte[] get_personal() {
            byte[] personal = new byte[BLAKE2B_PERSONAL_BYTES];
            System.arraycopy(param, 48, personal, 0, personal.length);
            return personal;
        }
    }

    /**
     * Blake2xb state,
     * defined in <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2.h#L137">blake2.h</a>.
     */
    static class Blake2xbState {
        /**
         * Blake2b state
         */
        Blake2bState S;
        /**
         * Blake2b parameter
         */
        Blake2bParam P;

        Blake2xbState() {
            S = new Blake2bState();
            P = new Blake2bParam();
        }
    }

    /**
     * Loads 32-bit integer from src, defined in
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2-impl.h#L33">blake2-impl.h</a>.
     *
     * @param src    src to load.
     * @param offset offset of src.
     * @return loaded 32-bit integer.
     */
    static int load32(byte[] src, int offset) {
        return Pack.littleEndianToInt(src, offset);
    }

    /**
     * Stores 32-bit integer into dst, defined in
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2-impl.h#L82">blake2-impl.h</a>.
     *
     * @param dst    dst to store.
     * @param offset offset of dst.
     * @param w      32-bit integer to store.
     */
    static void store32(byte[] dst, int offset, int w) {
        Pack.intToLittleEndian(w, dst, offset);
    }

    /**
     * Loads 64-bit integer from src, defined in
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2-impl.h#L45">blake2-impl.h</a>.
     *
     * @param src    src to load.
     * @param offset offset of src.
     * @return loaded 64-bit integer.
     */
    static long load64(byte[] src, int offset) {
        return Pack.littleEndianToLong(src, offset);
    }

    /**
     * Stores 64-bit integer into dst, defined in
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2-impl.h#L95">blake2-impl.h</a>.
     *
     * @param dst    dst to store.
     * @param offset offset of dst.
     * @param w      64-bit integer to store.
     */
    static void store64(byte[] dst, int offset, long w) {
        Pack.longToLittleEndian(w, dst, offset);
    }

    /**
     * utility function <code>rotr64</code>, defined in
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2-impl.h#L135">blake2-impl.h</a>.
     * As shown in
     * <a href="https://github.com/0xShamil/blake2b/blob/master/src/main/java/com/github/shamil/Blake2b.java#L566">Blake2b.java</a>,
     * <code>rotr64</code> is identical to <code>Long.rotateRight</code>.
     *
     * @param w the value whose bits are to be rotated right.
     * @param c the number of bit positions to rotate right.
     * @return the value obtained by rotating the two's complement binary representation of the specified <code>long</code>
     * value right by the specified number of bits.
     */
    static long rotr64(final long w, final int c) {
        return Long.rotateRight(w, c);
    }
}

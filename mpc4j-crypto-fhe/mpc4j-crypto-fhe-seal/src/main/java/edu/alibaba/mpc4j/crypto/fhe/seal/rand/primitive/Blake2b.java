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
 * More information about the BLAKE2 hash function can be found at
 * https://blake2.net.
 */
package edu.alibaba.mpc4j.crypto.fhe.seal.rand.primitive;

import edu.alibaba.mpc4j.crypto.fhe.seal.rand.primitive.Blake2.Blake2bParam;
import edu.alibaba.mpc4j.crypto.fhe.seal.rand.primitive.Blake2.Blake2bState;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.Common;
import org.bouncycastle.util.Pack;

import java.util.Arrays;

/**
 * Implementation of the cryptographic hash function Blake2b.
 * <p>
 * Blake2b offers a built-in keying mechanism to be used directly
 * for authentication ("Prefix-MAC") rather than a HMAC construction.
 * <p>
 * Blake2b offers a built-in support for a salt for randomized hashing
 * and a personal string for defining a unique hash function for each application.
 * <p>
 * BLAKE2b is optimized for 64-bit platforms and produces digests of any size
 * between 1 and 64 bytes.
 * <p>
 * The implementation is based on
 * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2b.c">blake2b.c</a>,
 * with several utility functions and comments from
 * <a href="https://github.com/0xShamil/blake2b/blob/master/src/main/java/com/github/shamil/Blake2b.java">Blake2b.java</a>.
 *
 * @author Weiran Liu
 * @date 2025/2/11
 */
public class Blake2b {
    /**
     * Blake2b Initialization Vector, Produced from the square root of primes 2, 3, 5, 7, 11, 13, 17, 19, the same as
     * SHA-512 IV.
     * <p>
     * <code>static const uint64_t blake2b_IV[8]</code> from
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2b.c#L23">blake2b.c</a>.
     */
    private static final long[] blake2b_IV = new long[]{
        0x6a09e667f3bcc908L, 0xbb67ae8584caa73bL,
        0x3c6ef372fe94f82bL, 0xa54ff53a5f1d36f1L,
        0x510e527fade682d1L, 0x9b05688c2b3e6c1fL,
        0x1f83d9abfb41bd6bL, 0x5be0cd19137e2179L,
    };

    /**
     * Message word permutations.
     * <p>
     * <code>static const uint8_t blake2b_sigma[12][16]</code> from
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2b.c#L31">blake2b.c</a>.
     */
    private static final byte[][] blake2b_sigma = new byte[][]{
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
        {14, 10, 4, 8, 9, 15, 13, 6, 1, 12, 0, 2, 11, 7, 5, 3},
        {11, 8, 12, 0, 5, 2, 15, 13, 10, 14, 3, 6, 7, 1, 9, 4},
        {7, 9, 3, 1, 13, 12, 11, 14, 2, 6, 5, 10, 4, 0, 15, 8},
        {9, 0, 5, 7, 2, 4, 10, 15, 14, 1, 11, 12, 6, 8, 3, 13},
        {2, 12, 6, 10, 0, 11, 8, 3, 4, 13, 7, 5, 15, 14, 1, 9},
        {12, 5, 1, 15, 14, 13, 4, 10, 0, 7, 6, 3, 9, 2, 8, 11},
        {13, 11, 7, 14, 12, 1, 3, 9, 5, 0, 15, 4, 8, 6, 2, 10},
        {6, 15, 14, 9, 11, 3, 0, 8, 12, 2, 13, 7, 1, 4, 10, 5},
        {10, 2, 8, 4, 7, 6, 1, 5, 15, 11, 9, 14, 3, 12, 13, 0},
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
        {14, 10, 4, 8, 9, 15, 13, 6, 1, 12, 0, 2, 11, 7, 5, 3},
    };

    /**
     * <code>static void blake2b_set_lastnode(blake2b_state *S)</code> from
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2b.c#L48">blake2b.c</a>.
     *
     * @param S Blake2b state.
     */
    private static void blake2b_set_last_node(Blake2bState S) {
        S.f[1] = -1L;
    }

    /**
     * <code>static int blake2b_is_lastblock(const blake2b_state *S)</code> from
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2b.c#L54">blake2b.c</a>.
     *
     * @param S Blake2b state.
     * @return if it is the last block.
     */
    private static boolean blake2b_is_last_block(Blake2bState S) {
        return S.f[0] != 0;
    }

    /**
     * <code>static void blake2b_set_lastblock(blake2b_state *S)</code> from
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2b.c#L59">blake2b.c</a>.
     *
     * @param S Blake2b state.
     */
    private static void blake2b_set_last_block(Blake2bState S) {
        if (S.last_node != 0) {
            blake2b_set_last_node(S);
        }
        S.f[0] = -1L;
    }

    /**
     * <code>static void blake2b_increment_counter(blake2b_state *S, const uint64_t inc)</code> from
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2b.c#L66">blake2b.c</a>.
     *
     * @param S   Blake2b state.
     * @param inc increment counter.
     */
    private static void blake2b_increment_counter(Blake2bState S, final long inc) {
        S.t[0] += inc;
        S.t[1] += (S.t[0] < inc ? 1 : 0);
    }

    /**
     * <code>static void blake2b_init0(blake2b_state *S)</code> from
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2b.c#L72">blake2b.c</a>.
     *
     * @param S Blake2b state.
     */
    private static void blake2b_init0(Blake2bState S) {
        int i;
        // memset(S, 0, sizeof(blake2b_state));
        S.set_empty_state();
        for (i = 0; i < 8; ++i) {
            S.h[i] = blake2b_IV[i];
        }
    }

    /**
     * Init xors IV with input parameter block.
     * <p>
     * <code>int blake2b_init_param(blake2b_state *S, const blake2b_param *P)</code> from
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2b.c#L81">blake2b.c</a>.
     *
     * @param S Blake2b state.
     * @param P Blake2b param.
     */
    static void blake2b_init_param(Blake2bState S, final Blake2bParam P) {
        int i;

        blake2b_init0(S);

        /* IV XOR ParamBlock */
        for (i = 0; i < 8; ++i) {
            S.h[i] ^= Blake2.load64(P.param, Long.BYTES * i);
        }

        S.out_len = P.get_digest_length();
    }

    /**
     * <code>int blake2b_init(blake2b_state *S, size_t outlen)</code> from
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2b.c#L98">blake2b.c</a>.
     *
     * @param S      Blake2b state.
     * @param outlen output length.
     */
    static void blake2b_init(Blake2bState S, int outlen) {
        // blake2b_param P[1];
        Blake2bParam P = new Blake2bParam();

        // if ((!outlen) || (outlen > BLAKE2B_OUTBYTES)) return -1;
        if ((outlen <= 0) || (outlen > Blake2.BLAKE2B_OUT_BYTES)) {
            throw new IllegalArgumentException("outlen should be in range (0, " + Blake2.BLAKE2B_OUT_BYTES + "]: " + outlen);
        }

        // P->digest_length = (uint8_t) outlen;
        P.set_digest_length(outlen);
        // P->key_length    = 0;
        P.set_key_length(0);
        // P->fanout        = 1;
        P.set_fanout(1);
        // P->depth         = 1;
        P.set_depth(1);
        // store32(&P->leaf_length, 0);
        P.set_leaf_length(0);
        // store32(&P->node_offset, 0);
        P.set_node_offset(0);
        // store32(&P->xof_length, 0);
        P.set_xof_length(0);
        // P->node_depth    = 0;
        P.set_node_depth(0);
        // P->inner_length  = 0;
        P.set_inner_length(0);
        // memset(P->reserved, 0, sizeof(P->reserved));
        P.set_empty_reserved();
        // memset(P->salt,     0, sizeof(P->salt));
        P.set_empty_salt();
        // memset(P->personal, 0, sizeof(P->personal));
        P.set_empty_personal();
        blake2b_init_param(S, P);
    }

    /**
     * <code>int blake2b_init_key(blake2b_state *S, size_t outlen, const void *key, size_t keylen)</code> from
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2b.c#L120">blake2b.c</a>.
     *
     * @param S      Blake2b state.
     * @param outlen output length.
     * @param key    key.
     */
    static void blake2b_init_key(Blake2bState S, int outlen, final byte[] key) {
        // blake2b_param P[1];
        Blake2bParam P = new Blake2bParam();

        // if ((!outlen) || (outlen > BLAKE2B_OUTBYTES)) return -1;
        if ((outlen <= 0) || (outlen > Blake2.BLAKE2B_OUT_BYTES)) {
            throw new IllegalArgumentException(
                "outlen should be in range (0, " + Blake2.BLAKE2B_OUT_BYTES + "]: " + outlen
            );
        }

        // if (!key || !keylen || keylen > BLAKE2B_KEYBYTES) return -1;
        if (key == null || key.length == 0 || key.length > Blake2.BLAKE2B_KEY_BYTES) {
            throw new IllegalArgumentException(
                "key is null or keylen should be in range (0, " + Blake2.BLAKE2B_KEY_BYTES + "]"
            );
        }

        // P->digest_length = (uint8_t) outlen;
        P.set_digest_length(outlen);
        // P->key_length    = (uint8_t) keylen;
        P.set_key_length(key.length);
        // P->fanout        = 1;
        P.set_fanout(1);
        // P->depth         = 1;
        P.set_depth(1);
        // store32(&P->leaf_length, 0);
        P.set_leaf_length(0);
        // store32(&P->node_offset, 0);
        P.set_node_offset(0);
        // store32(&P->xof_length, 0);
        P.set_xof_length(0);
        // P->node_depth    = 0;
        P.set_node_depth(0);
        // P->inner_length  = 0;
        P.set_inner_length(0);
        // memset(P->reserved, 0, sizeof(P->reserved));
        P.set_empty_reserved();
        // memset(P->salt,     0, sizeof(P->salt));
        P.set_empty_salt();
        // memset(P->personal, 0, sizeof(P->personal));
        P.set_empty_personal();

        blake2b_init_param(S, P);

        byte[] block = new byte[Blake2.BLAKE2B_BLOCK_BYTES];
        System.arraycopy(key, 0, block, 0, key.length);
        blake2b_update(S, block);
        /* Burn the key from stack */
        // secure_zero_memory( block, BLAKE2B_BLOCKBYTES );
        Arrays.fill(block, (byte) 0);
    }

    /**
     * <code>#define G(r,i,a,b,c,d)</code> from
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2b.c#L153">blake2b.c</a>. Here
     * we use the implementation shown in
     * <a href="https://github.com/0xShamil/blake2b/blob/master/src/main/java/com/github/shamil/Blake2b.java#L445">Blake2b.java</a>
     * with modification that treating the internal state as a variable.
     *
     * @param internalState internal state.
     * @param m1            value of m[blake2b_sigma[r][2*i+0]].
     * @param m2            value of m[blake2b_sigma[r][2*i+1]].
     * @param posA          position of a in the internal state.
     * @param posB          position of b in the internal state.
     * @param posC          position of c in the internal state.
     * @param posD          position of d in the internal state.
     */
    private static void G(long[] internalState, long m1, long m2, int posA, int posB, int posC, int posD) {
        assert internalState.length == 16;
        // a = a + b + m[blake2b_sigma[r][2*i+0]];
        internalState[posA] = internalState[posA] + internalState[posB] + m1;
        // d = rotr64(d ^ a, 32);
        internalState[posD] = Blake2.rotr64(internalState[posD] ^ internalState[posA], 32);
        // c = c + d;
        internalState[posC] = internalState[posC] + internalState[posD];
        // b = rotr64(b ^ c, 24);
        internalState[posB] = Blake2.rotr64(internalState[posB] ^ internalState[posC], 24);
        // a = a + b + m[blake2b_sigma[r][2*i+1]];
        internalState[posA] = internalState[posA] + internalState[posB] + m2;
        // d = rotr64(d ^ a, 16);
        internalState[posD] = Blake2.rotr64(internalState[posD] ^ internalState[posA], 16);
        // c = c + d;
        internalState[posC] = internalState[posC] + internalState[posD];
        // b = rotr64(b ^ c, 63);
        internalState[posB] = Blake2.rotr64(internalState[posB] ^ internalState[posC], 63);
    }

    /**
     * <code>static void blake2b_compress(blake2b_state *S, const uint8_t block[BLAKE2B_BLOCKBYTES])</code> from
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2b.c#L177">blake2b.c</a>.
     *
     * @param S      Blake2b state.
     * @param block  message block.
     * @param offset offset of message block.
     */
    private static void blake2b_compress(Blake2bState S, byte[] block, int offset) {
        // uint64_t m[16];
        long[] m = new long[16];
        // uint64_t v[16];
        long[] v = new long[16];
        // size_t i;
        int i;

        for (i = 0; i < 16; i++) {
            // m[i] = load64(block + i * sizeof(m[i]));
            m[i] = Blake2.load64(block, offset + i * 8);
        }

        for (i = 0; i < 8; i++) {
            // v[i] = S->h[i];
            v[i] = S.h[i];
        }

        v[8] = blake2b_IV[0];
        v[9] = blake2b_IV[1];
        v[10] = blake2b_IV[2];
        v[11] = blake2b_IV[3];
        v[12] = blake2b_IV[4] ^ S.t[0];
        v[13] = blake2b_IV[5] ^ S.t[1];
        v[14] = blake2b_IV[6] ^ S.f[0];
        v[15] = blake2b_IV[7] ^ S.f[1];

        for (int round = 0; round < 12; round++) {
            G(v, m[blake2b_sigma[round][0]], m[blake2b_sigma[round][1]], 0, 4, 8, 12);
            G(v, m[blake2b_sigma[round][2]], m[blake2b_sigma[round][3]], 1, 5, 9, 13);
            G(v, m[blake2b_sigma[round][4]], m[blake2b_sigma[round][5]], 2, 6, 10, 14);
            G(v, m[blake2b_sigma[round][6]], m[blake2b_sigma[round][7]], 3, 7, 11, 15);
            // G apply to diagonals of internalState:
            G(v, m[blake2b_sigma[round][8]], m[blake2b_sigma[round][9]], 0, 5, 10, 15);
            G(v, m[blake2b_sigma[round][10]], m[blake2b_sigma[round][11]], 1, 6, 11, 12);
            G(v, m[blake2b_sigma[round][12]], m[blake2b_sigma[round][13]], 2, 7, 8, 13);
            G(v, m[blake2b_sigma[round][14]], m[blake2b_sigma[round][15]], 3, 4, 9, 14);
        }

        for (i = 0; i < 8; i++) {
            // S->h[i] = S->h[i] ^ v[i] ^ v[i + 8];
            S.h[i] = S.h[i] ^ v[i] ^ v[i + 8];
        }
    }

    static void blake2b_update(Blake2bState S, byte[] in) {
        blake2b_update(S, in, in == null ? 0 : in.length);
    }

    /**
     * <code>int blake2b_update(blake2b_state *S, const void *pin, size_t inlen)</code> from
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2b.c#L221">blake2b.c</a>.
     *
     * @param S  Blake2b state.
     * @param in input.
     */
    static void blake2b_update(Blake2bState S, byte[] in, int inlen) {
        int inOffset = 0;
        if (inlen > 0) {
            int left = S.buf_len;
            int fill = Blake2.BLAKE2B_BLOCK_BYTES - left;
            if (inlen > fill) {
                S.buf_len = 0;
                System.arraycopy(in, inOffset, S.buf, left, fill);
                blake2b_increment_counter(S, Blake2.BLAKE2B_BLOCK_BYTES);
                blake2b_compress(S, S.buf, 0);
                inOffset += fill;
                inlen -= fill;
                while (inlen > Blake2.BLAKE2B_BLOCK_BYTES) {
                    blake2b_increment_counter(S, Blake2.BLAKE2B_BLOCK_BYTES);
                    blake2b_compress(S, in, inOffset);
                    inOffset += Blake2.BLAKE2B_BLOCK_BYTES;
                    inlen -= Blake2.BLAKE2B_BLOCK_BYTES;
                }
            }
            System.arraycopy(in, inOffset, S.buf, S.buf_len, inlen);
            S.buf_len += inlen;
        }
    }

    static void blake2b_final(Blake2bState S, byte[] out) {
        blake2b_final(S, out, 0);
    }

    /**
     * <code>int blake2b_final(blake2b_state *S, void *out, size_t outlen)</code> from
     * <a href="https://github.com/microsoft/SEAL/blob/main/native/src/seal/util/blake2b.c#L248">blake2b.c</a>.
     *
     * @param S   Blake2b state.
     * @param out output buffer.
     * @param outOffset output offset.
     */
    static void blake2b_final(Blake2bState S, byte[] out, int outOffset) {
        byte[] buffer = new byte[Blake2.BLAKE2B_OUT_BYTES];
        int i;

        // if (out == NULL || outlen < S->outlen) return -1
        if (out == null) {
            throw new IllegalArgumentException("out is null");
        }
        int outlen = out.length - outOffset;
        if (outlen <= 0 || outlen < S.out_len) {
            throw new IllegalArgumentException(
                "out is null or outlen should be in range (0, " + S.out_len + "]"
            );
        }

        // if (blake2b_is_lastblock(S)) return -1;
        if (blake2b_is_last_block(S)) {
            throw new IllegalArgumentException("Blake2b state should not be last_block");
        }

        blake2b_increment_counter(S, S.buf_len);
        blake2b_set_last_block(S);
        // padding: memset(S->buf + S->buflen, 0, BLAKE2B_BLOCKBYTES - S->buflen);
        Arrays.fill(S.buf, S.buf_len, Blake2.BLAKE2B_BLOCK_BYTES, (byte) 0);
        blake2b_compress(S, S.buf, 0);

        /* Output full hash to temp buffer */
        for (i = 0; i < 8; ++i) {
            // store64(buffer + sizeof(S->h[i]) * i, S->h[i]);
            Blake2.store64(buffer, Long.BYTES * i, S.h[i]);
        }

        // memcpy(out, buffer, S->outlen);
        System.arraycopy(buffer, 0, out, outOffset, S.out_len);
        // secure_zero_memory(buffer, sizeof(buffer));
        Arrays.fill(buffer, (byte) 0);
    }

    /**
     * Blake2b with given input and key.
     *
     * @param out output buffer.
     * @param in  input, can be null.
     * @param key key, can be null.
     */
    static void blake2b(byte[] out, final byte[] in, final byte[] key) {
        Blake2bState S = new Blake2bState();

        /* Verify parameters */
        // if (NULL == in && inlen > 0) return -1; This assert can be ignored.
        // if (NULL == out) return -1;
        if (out == null) {
            throw new IllegalArgumentException("out is null");
        }

        // if (NULL == key && keylen > 0) return -1; This assert can be ignored.
        // if (!outlen || outlen > BLAKE2B_OUTBYTES) return -1;
        if (out.length == 0 || out.length > Blake2.BLAKE2B_OUT_BYTES) {
            throw new IllegalArgumentException(
                "outlen should be in range (0, " + Blake2.BLAKE2B_OUT_BYTES + "]: " + out.length
            );
        }

        // if (keylen > BLAKE2B_KEYBYTES) return -1;
        if (key != null && (key.length == 0 || key.length > Blake2.BLAKE2B_KEY_BYTES)) {
            throw new IllegalArgumentException(
                "keylen should be in range (0, " + Blake2.BLAKE2B_KEY_BYTES + "]: " + key.length
            );
        }

        if (key != null) {
            blake2b_init_key(S, out.length, key);
        } else {
            blake2b_init(S, out.length);
        }

        blake2b_update(S, in);
        blake2b_final(S, out);
    }

    /**
     * Computes the hash for the given long array. This is only used for computing encryption parameter ID. See:
     * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/hash.h#L32">hash.h</a>.
     *
     * @param destination destination.
     * @param in input long array.
     * @param uint64Count number of longs to hash.
     */
    public static void blake2b(long[] destination, long[] in, int uint64Count) {
        byte[] input = new byte[uint64Count * Common.BYTES_PER_UINT64];
        for (int i = 0; i < uint64Count; i++) {
            Pack.longToLittleEndian(in[i], input, i * Common.BYTES_PER_UINT64);
        }
        byte[] output = new byte[destination.length * Common.BYTES_PER_UINT64];
        blake2b(output, input, null);
        Pack.littleEndianToLong(output, 0, destination);
    }
}

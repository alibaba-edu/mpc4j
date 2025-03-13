/*
 * BLAKE2 reference source code package - reference C implementations
 *
 * Copyright 2016, JP Aumasson <jeanphilippe.aumasson@gmail.com>.
 * Copyright 2016, Samuel Neves <sneves@dei.uc.pt>.
 *
 * You may use this under the terms of the CC0, the OpenSSL Licence, or
 * the Apache Public License 2.0, at your option.  The terms of these
 * licenses can be found at:
 *
 * - CC0 1.0 Universal : http://creativecommons.org/publicdomain/zero/1.0
 * - OpenSSL license   : https://www.openssl.org/source/license.html
 * - Apache 2.0        : http://www.apache.org/licenses/LICENSE-2.0
 *
 * More information about the BLAKE2 hash function can be found at
 * https://blake2.net.
 */

package edu.alibaba.mpc4j.crypto.fhe.seal.rand.primitive;

import edu.alibaba.mpc4j.crypto.fhe.seal.rand.UniformRandomGeneratorFactory;
import edu.alibaba.mpc4j.crypto.fhe.seal.rand.primitive.Blake2.Blake2bParam;
import edu.alibaba.mpc4j.crypto.fhe.seal.rand.primitive.Blake2.Blake2bState;
import edu.alibaba.mpc4j.crypto.fhe.seal.rand.primitive.Blake2.Blake2xbState;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.Common;
import org.bouncycastle.util.Pack;

import java.util.Arrays;

/**
 * @author Weiran Liu
 * @date 2025/2/12
 */
public class Blake2xb {

    private static void blake2xb_init_key(Blake2xbState S, final int outlen, byte[] key) {
        // if (outlen == 0 || outlen > 0xFFFFFFFFUL) return -1;
        if (outlen <= 0) {
            throw new IllegalArgumentException("outlen must be greater than 0: " + outlen);
        }

        // if (NULL != key && keylen > BLAKE2B_KEYBYTES) return -1;
        if (key != null && key.length > Blake2.BLAKE2B_KEY_BYTES) {
            throw new IllegalArgumentException("keylen must be in range [0, " + Blake2.BLAKE2B_KEY_BYTES + "]: " + key.length);
        }

        // if (NULL == key && keylen > 0) return -1; This assertion can be ignored.

        /* Initialize parameter block */
        // S->P->digest_length = BLAKE2B_OUTBYTES;
        S.P.set_digest_length(Blake2.BLAKE2B_OUT_BYTES);

        // S -> P -> key_length = (uint8_t) keylen;
        S.P.set_key_length(key == null ? 0 : key.length);

        // S -> P -> fanout = 1;
        S.P.set_fanout(1);
        // S -> P -> depth = 1;
        S.P.set_depth(1);
        // store32( & S -> P -> leaf_length, 0 );
        S.P.set_leaf_length(0);
        // store32( & S -> P -> node_offset, 0 );
        S.P.set_node_offset(0);

        // store32( & S -> P -> xof_length, (uint32_t) outlen );
        S.P.set_xof_length(outlen);

        // S -> P -> node_depth = 0;
        S.P.set_node_depth(0);
        // S -> P -> inner_length = 0;
        S.P.set_inner_length(0);
        // memset(S -> P -> reserved, 0, sizeof(S -> P -> reserved));
        S.P.set_empty_reserved();
        // memset(S -> P -> salt, 0, sizeof(S -> P -> salt));
        S.P.set_empty_salt();
        // memset(S -> P -> personal, 0, sizeof(S -> P -> personal));
        S.P.set_empty_personal();

        Blake2b.blake2b_init_param(S.S, S.P);

        if (key != null && key.length > 0) {
            byte[] block = new byte[Blake2.BLAKE2B_BLOCK_BYTES];
            System.arraycopy(key, 0, block, 0, key.length);
            Blake2b.blake2b_update(S.S, block);
            // secure_zero_memory(block, BLAKE2B_BLOCKBYTES);
            Arrays.fill(block, (byte) 0);
        }
    }

    private static void blake2xb_update(Blake2xbState S, final byte[] in) {
        Blake2b.blake2b_update(S.S, in);
    }

    private static void blake2xb_final(Blake2xbState S, byte[] out) {
        Blake2bState C = new Blake2bState();
        Blake2bParam P = new Blake2bParam();
        // load32(&S->P->xof_length);
        int xof_length = S.P.get_xof_length();
        byte[] root = new byte[Blake2.BLAKE2B_BLOCK_BYTES];
        int i;

        // if (NULL == out) return -1
        if (out == null) {
            throw new IllegalArgumentException("out is null");
        }

        int outlen = out.length;
        /* outlen must match the output size defined in xof_length, */
        /* unless it was -1, in which case anything goes except 0. */
        if (xof_length == 0xFFFFFFFF) {
            // if (outlen == 0) return -1
            if (outlen == 0) {
                throw new IllegalArgumentException("outlen must not be 0:" + outlen);
            }
        } else {
            // if (outlen != xof_length) return -1
            if (outlen != xof_length) {
                throw new IllegalArgumentException("outlen must not be " + xof_length + ": " + outlen);
            }
        }

        /* Finalize the root hash */
        Blake2b.blake2b_final(S.S, root);

        /* Set common block structure values */
        /* Copy values from parent instance, and only change the ones below */
        // memcpy(P, S -> P, sizeof(blake2b_param));
        System.arraycopy(S.P.param, 0, P.param, 0, Blake2bParam.BLAKE2B_PARAM_BYTES);
        // P -> key_length = 0;
        P.set_key_length(0);
        // P -> fanout = 0;
        P.set_fanout(0);
        // P -> depth = 0;
        P.set_depth(0);
        // store32( & P -> leaf_length, BLAKE2B_OUTBYTES);
        P.set_leaf_length(Blake2.BLAKE2B_OUT_BYTES);
        // P -> inner_length = BLAKE2B_OUTBYTES;
        P.set_inner_length(Blake2.BLAKE2B_OUT_BYTES);
        // P -> node_depth = 0;
        P.set_node_depth(0);

        for (i = 0; outlen > 0; ++i) {
            // const size_t block_size = (outlen < BLAKE2B_OUTBYTES) ? outlen : BLAKE2B_OUTBYTES;
            final int block_size = Math.min(outlen, Blake2.BLAKE2B_OUT_BYTES);
            /* Initialize state */

            // P -> digest_length = (uint8_t) block_size;
            P.set_digest_length(block_size);
            // store32( & P -> node_offset, (uint32_t) i);
            P.set_node_offset(i);

            Blake2b.blake2b_init_param(C, P);
            /* Process key if needed */
            Blake2b.blake2b_update(C, root, Blake2.BLAKE2B_OUT_BYTES);
            // blake2b_final(C, (uint8_t *)out + i * BLAKE2B_OUTBYTES, block_size)
            Blake2b.blake2b_final(C, out, i * Blake2.BLAKE2B_OUT_BYTES);
            outlen -= block_size;
        }
        // secure_zero_memory(root, sizeof(root));
        Arrays.fill(root, (byte) 0);
        // secure_zero_memory(P, sizeof(P));
        P.set_empty_param();
        // secure_zero_memory(C, sizeof(C));
        C.set_empty_state();
    }

    static void blake2xb(byte[] out, final byte[] in, final byte[] key) {
        Blake2xbState S = new Blake2xbState();

        /* Verify parameters */
        // if (NULL == in && inlen > 0) return -1; This assertion can be ignored.
        // if (NULL == out) return -1;
        if (out == null) {
            throw new IllegalArgumentException("out is null");
        }

        // if (NULL == key && keylen > 0) return -1; This assertion can be ignored.
        // if (keylen > BLAKE2B_KEYBYTES) return -1;
        if (key != null && key.length > Blake2.BLAKE2B_KEY_BYTES) {
            throw new IllegalArgumentException(
                "keylen must be in range [0, " + Blake2.BLAKE2B_KEY_BYTES + "]: " + key.length
            );
        }

        // if (outlen == 0) return -1;
        if (out.length == 0) {
            throw new IllegalArgumentException("outlen must be greater than 0");
        }

        /* Initialize the root block structure */
        blake2xb_init_key(S, out.length, key);

        /* Absorb the input message */
        blake2xb_update(S, in);

        /* Compute the root node of the tree and the final hash using the counter construction */
        blake2xb_final(S, out);
    }

    public static void blake2xb(byte[] out, final long[] seed, final long counter) {
        assert seed.length == UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT;
        byte[] key = new byte[seed.length * Common.BYTES_PER_UINT64];
        Pack.longToLittleEndian(seed, key, 0);
        byte[] in = new byte[Common.BYTES_PER_UINT64];
        Pack.longToLittleEndian(counter, in, 0);
        blake2xb(out, in, key);
    }
}

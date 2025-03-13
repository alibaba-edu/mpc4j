/*
 * This file is a part of the Kyber library (https://github.com/pq-crystals/kyber)
 * commit 844057468e69527bd15b17fbe03f4b61f9a22065. The Kyber library is licensed
 * under CC0 Universal, version 1.0. You can find a copy of this license at
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 *
 * Modifications are done to make the implementation in pure Java.
 */
package edu.alibaba.mpc4j.crypto.fhe.seal.rand.primitive;

import edu.alibaba.mpc4j.crypto.fhe.seal.rand.UniformRandomGeneratorFactory;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.Common;
import org.bouncycastle.crypto.digests.SHAKEDigest;
import org.bouncycastle.util.Pack;

/**
 * Standalone SHAKE256 implementation.
 * <p>
 * Based on the public domain implementation in crypto_hash/keccakc512/simple/ from
 * <a href="http://bench.cr.yp.to/supercop.html">supercop.html</a>
 * by Ronny Van Keer and the public domain "TweetFips202" implementation from
 * <a href="https://twitter.com/tweetfips202">tweetfips202</a>.
 * by Gilles Van Assche, Daniel J. Bernstein, and Peter Schwabe.
 * <p>
 * The source code is modified from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/util/fips202.c">fips202.c</a>.
 *
 * @author Weiran Liu
 * @date 2025/2/10
 */
public class Shake256 {
    /**
     * SHAKE256 XOF with non-incremental API.
     *
     * @param out output.
     * @param in  input.
     */
    static void shake256(byte[] out, final byte[] in) {
        SHAKEDigest shakeDigest = new SHAKEDigest(256);
        shakeDigest.update(in, 0, in.length);
        shakeDigest.doFinal(out, 0, out.length);
    }

    /**
     * SHAKE256 PRNG.
     *
     * @param out     output.
     * @param seed    seed.
     * @param counter counter.
     */
    public static void shake256(byte[] out, final long[] seed, final long counter) {
        assert seed.length == UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT;
        byte[] extendSeed = new byte[seed.length * Common.BYTES_PER_UINT64 + Common.BYTES_PER_UINT64];
        Pack.longToLittleEndian(seed, extendSeed, 0);
        Pack.longToLittleEndian(counter, extendSeed, seed.length * Common.BYTES_PER_UINT64);
        shake256(out, extendSeed);
    }
}

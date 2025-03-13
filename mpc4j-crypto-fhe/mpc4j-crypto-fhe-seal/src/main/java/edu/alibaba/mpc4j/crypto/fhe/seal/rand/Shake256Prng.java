package edu.alibaba.mpc4j.crypto.fhe.seal.rand;

import edu.alibaba.mpc4j.crypto.fhe.seal.rand.primitive.Shake256;

/**
 * SHAKE256 pseudo-random generator.
 *
 * @author Weiran Liu
 * @date 2025/2/13
 */
class Shake256Prng extends AbstractPrng {

    Shake256Prng() {
        super();
    }

    public Shake256Prng(long[] seed) {
        super(seed);
    }

    @Override
    protected void refillBuffer() {
        Shake256.shake256(buffer, seed, counter);
        counter++;
    }

    @Override
    public PrngType getType() {
        return PrngType.SHAKE256;
    }
}

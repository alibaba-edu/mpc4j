package edu.alibaba.mpc4j.crypto.fhe.seal.rand;

import edu.alibaba.mpc4j.crypto.fhe.seal.rand.primitive.Blake2xb;

/**
 * Blake2xb pseudo-random generator.
 *
 * @author Weiran Liu
 * @date 2025/2/13
 */
class Blake2xbPrng extends AbstractPrng {

    Blake2xbPrng() {
        super();
    }

    public Blake2xbPrng(long[] seed) {
        super(seed);
    }

    @Override
    protected void refillBuffer() {
        Blake2xb.blake2xb(buffer, seed, counter);
        counter++;
    }

    @Override
    public PrngType getType() {
        return PrngType.BLAKE2XB;
    }
}

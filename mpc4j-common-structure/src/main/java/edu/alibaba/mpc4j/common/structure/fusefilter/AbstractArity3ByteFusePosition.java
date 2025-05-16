package edu.alibaba.mpc4j.common.structure.fusefilter;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

/**
 * abstract byte fuse position with arity = 3
 *
 * @author Weiran Liu
 * @date 2024/7/26
 */
abstract class AbstractArity3ByteFusePosition<T> extends AbstractArity3ByteFuseInstance implements ByteFusePosition<T> {
    /**
     * seed
     */
    protected final byte[] seed;
    /**
     * hash
     */
    protected final Prf hash;

    AbstractArity3ByteFusePosition(EnvType envType, int size, int valueByteLength) {
        super(size, valueByteLength);
        // here we do not input seed, since we need to decide seed internally in Byte Fuse Filter.
        hash = PrfFactory.createInstance(envType, Long.BYTES);
        seed = BlockUtils.zeroBlock();
    }

    @Override
    public byte[] seed() {
        return seed;
    }

    @Override
    public int[] positions(T x) {
        long hash = hash(x);
        int h0 = ByteFuseUtils.reduce((int) (hash >>> 32), segmentCountLength);
        int h1 = h0 + segmentLength;
        int h2 = h1 + segmentLength;
        h1 ^= (int) ((hash >> 18) & segmentLengthMask);
        h2 ^= (int) ((hash) & segmentLengthMask);
        return new int[]{h0, h1, h2};
    }

    /**
     * Computes hash of the input.
     *
     * @param x input x.
     * @return hash(x).
     */
    protected long hash(T x) {
        return LongUtils.byteArrayToLong(hash.getBytes(ObjectUtils.objectToByteArray(x)));
    }

    /**
     * Computes position for the given index and the hash of the input x. Each position consists of two parts:
     * <li>segment index h: range in [0, segmentCountLength). Given the same hash of the input x, segment index h for
     * must be consecutive, i.e., (h, 2 * segmentLength + h, 3 * segmentLength + h).</li>
     * <li>segment value hh: range in [0, segmentLength), where segmentLength must be a power-of-2 value. Given the
     * same hash of the input x, segment value hh must be (0, hh1, hh2), where hh1 and hh2 are two random values.</li>
     *
     * @param hash  hash.
     * @param index index.
     * @return position.
     */
    protected int getHashFromHash(long hash, int index) {
        long h = ByteFuseUtils.reduce((int) (hash >>> 32), segmentCountLength);
        h += (long) index * segmentLength;
        // keep the lower 36 bits
        long hh = hash & ((1L << 36) - 1);
        // index 0: right shift by 36; index 1: right shift by 18; index 2: no shift
        h ^= (int) ((hh >>> (36 - 18 * index)) & segmentLengthMask);
        return (int) h;
    }
}

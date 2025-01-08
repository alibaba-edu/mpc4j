package edu.alibaba.mpc4j.common.structure.fusefilter;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * abstract byte fuse instance with arity = 3.
 *
 * @author Weiran Liu
 * @date 2024/9/2
 */
abstract class AbstractArity3ByteFuseInstance implements ByteFuseInstance {
    /**
     * arity, i.e., number of positions.
     */
    static final int ARITY = 3;
    /**
     * segment count.
     */
    protected final int segmentCount;
    /**
     * segment count length.
     */
    protected final int segmentCountLength;
    /**
     * segment length, must be a power-of-2.
     */
    protected final int segmentLength;
    /**
     * segment length mask, used for fast module.
     */
    protected final int segmentLengthMask;
    /**
     * filter length, i.e., the length of the filter.
     */
    protected final int filterLength;
    /**
     * value byte length
     */
    protected final int valueByteLength;

    AbstractArity3ByteFuseInstance(int size, int valueByteLength) {
        MathPreconditions.checkPositive("size", size);
        // segment length must be a power-of-2
        int segmentLength = ByteFuseUtils.calculateArity3SegmentLength(size);
        // the current implementation hardcodes a 18-bit limit to the segment length.
        if (segmentLength > (1 << 18)) {
            segmentLength = (1 << 18);
        }
        double sizeFactor = ByteFuseUtils.calculateArity3SizeFactor(size);
        // calculate capacity: (1) N = c * m; (2) round N to divide segment length; (3) calculate number of segments
        int capacity = (int) (size * sizeFactor);
        int segmentCount = (capacity + segmentLength - 1) / segmentLength - (ARITY - 1);
        int arrayLength = (segmentCount + ARITY - 1) * segmentLength;
        segmentCount = (arrayLength + segmentLength - 1) / segmentLength;
        // make sure that segment count must be a positive value
        segmentCount = segmentCount <= ARITY - 1 ? 1 : segmentCount - (ARITY - 1);

        // set parameters
        if (segmentLength < 0 || Integer.bitCount(segmentLength) != 1) {
            throw new IllegalArgumentException("Segment length needs to be a power of 2, is " + segmentLength);
        }

        MathPreconditions.checkPositive("value_byte_length", valueByteLength);
        this.segmentLength = segmentLength;
        this.segmentCount = segmentCount;
        segmentLengthMask = segmentLength - 1;
        segmentCountLength = segmentCount * segmentLength;
        filterLength = (segmentCount + ARITY - 1) * segmentLength;
        this.valueByteLength = valueByteLength;
    }

    @Override
    public int arity() {
        return ARITY;
    }

    @Override
    public int valueByteLength() {
        return valueByteLength;
    }

    @Override
    public int filterLength() {
        return filterLength;
    }
}

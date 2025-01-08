/*
 * HPPC
 *
 * Copyright (C) 2010-2022 Carrot Search s.c.
 * All rights reserved.
 *
 * Refer to the full license file "LICENSE.txt":
 * https://github.com/carrotsearch/hppc/blob/master/LICENSE.txt
 */
package edu.alibaba.mpc4j.common.structure.pgm;

/**
 * Utility methods for PGM-index.
 *
 * <p> Forked from HPPC commit c9497dfabff240787aa0f5ac7a8f4ad70117ea72, make the constructor private, remove KType.
 *
 * <p> Although HPCC contains this implementation, it is in package-private state so that we have to fork it.
 *
 * @author Weiran Liu
 * @date 2024/7/28
 */
final class PgmIndexUtil {
    /**
     * private constructor.
     */
    private PgmIndexUtil() {
        // empty
    }

    /**
     * Adds the first key of the current segment to the segment data bytes.
     */
    static void addLongKey(long key, IntGrowableArray segmentData) {
        segmentData.add((int) key);
        segmentData.add((int) (key >>> 32));
    }

    /**
     * Adds the first key of the current segment to the segment data bytes.
     */
    static void addDoubleKey(double key, IntGrowableArray segmentData) {
        addLongKey(Double.doubleToRawLongBits(key), segmentData);
    }

    /**
     * Gets the first key of the segment at the given data index.
     */
    static long getLongKey(int segmentDataIndex, int[] segmentData) {
        return (segmentData[segmentDataIndex] & 0xFFFFFFFFL) | (((long) segmentData[segmentDataIndex + 1]) << 32);
    }

    /**
     * Gets the first key of the segment at the given data index.
     */
    static double getDoubleKey(int segmentDataIndex, int[] segmentData) {
        return Double.longBitsToDouble(getLongKey(segmentDataIndex, segmentData));
    }

    /**
     * Adds the intercept of the current segment to the segment data bytes. The intercept is stored as
     * an int for a key size equal to 1, otherwise it is stored as a long.
     */
    static void addIntercept(long intercept, IntGrowableArray segmentData) {
        addLongKey(intercept, segmentData);
    }

    /**
     * Gets the intercept of the segment at the given data index.\
     */
    static long getIntercept(int segmentDataIndex, int[] segmentData) {
        return getLongKey(segmentDataIndex, segmentData);
    }

    /**
     * Adds the slope of the current segment to the segment data bytes. The intercept is stored as a
     * float for a key size equal to 1, otherwise it is stored as a double.
     */
    static void addSlope(double slope, IntGrowableArray segmentData) {
        addDoubleKey(slope, segmentData);
    }

    /**
     * Gets the slope of the segment at the given data index.
     */
    static double getSlope(int segmentDataIndex, int[] segmentData) {
        return getDoubleKey(segmentDataIndex, segmentData);
    }
}

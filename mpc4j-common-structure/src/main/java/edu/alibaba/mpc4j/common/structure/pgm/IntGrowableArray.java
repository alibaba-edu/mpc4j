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

import com.carrotsearch.hppc.BoundedProportionalArraySizingStrategy;

import java.util.Arrays;

/**
 * Basic growable int array helper for HPPC templates (so before {@code IntArrayList} is generated).
 *
 * <p> Forked from HPPC commit c9497dfabff240787aa0f5ac7a8f4ad70117ea72.
 *
 * <p> This is only used to construct PGM-index. As shown in
 * <a href="https://github.com/carrotsearch/hppc/pull/39#">#39</a>, bruno-roustant stats that:
 * <pre>
 *     Indeed this PGM-Index is not a collection in itself (though there is a "dynamic" version of it in the paper
 *     that becomes a collection, but it's closer to a Lucene index than a collection for additions and removal). It
 *     clearly benefits from the cool template generation platform, but I agree that it could be moved to a separate
 *     module.
 * </pre>
 * Therefore, the PGM-index (together with IntGrowableArray) is not merged into HPCC itself.
 *
 * @author Weiran Liu
 * @date 2024/7/28
 */
public class IntGrowableArray {
    /**
     * array buffer
     */
    public int[] buffer;
    /**
     * size
     */
    public int size;

    /**
     * Creates an empty int growable array with the given initial capacity.
     *
     * @param initialCapacity initial capacity.
     */
    public IntGrowableArray(int initialCapacity) {
        buffer = new int[initialCapacity];
    }

    /**
     * Adds a data into the array.
     *
     * @param e data
     */
    public void add(int e) {
        ensureBufferSpace();
        buffer[size++] = e;
    }

    /**
     * Gets data stored in the array.
     *
     * @return data stored in the array.
     */
    public int[] toArray() {
        return buffer.length == size ? buffer : Arrays.copyOf(buffer, size);
    }

    private void ensureBufferSpace() {
        if (size + 1 > buffer.length) {
            int newSize = BoundedProportionalArraySizingStrategy.DEFAULT_INSTANCE.grow(buffer.length, size, 1);
            buffer = Arrays.copyOf(buffer, newSize);
        }
    }
}

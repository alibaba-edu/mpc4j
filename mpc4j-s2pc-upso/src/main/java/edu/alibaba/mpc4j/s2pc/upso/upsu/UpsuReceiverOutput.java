package edu.alibaba.mpc4j.s2pc.upso.upsu;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * UPSU receiver output.
 *
 * @author Liqiang Peng
 * @date 2024/3/20
 */
public class UpsuReceiverOutput {
    /**
     * union set
     */
    private final Set<ByteBuffer> unionSet;
    /**
     * intersection set size
     */
    private final int intersectionSetSize;

    public UpsuReceiverOutput(Set<ByteBuffer> unionSet, int intersectionSetSize) {
        this.unionSet = unionSet;
        this.intersectionSetSize = intersectionSetSize;
    }

    /**
     * return union set.
     *
     * @return union set
     */
    public Set<ByteBuffer> getUnionSet() {
        return unionSet;
    }

    /**
     * return intersection set size.
     *
     * @return intersection set size.
     */
    public int getIntersectionSetSize() {
        return intersectionSetSize;
    }
}

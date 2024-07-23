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
     * PSI-CA
     */
    private final int psica;

    public UpsuReceiverOutput(Set<ByteBuffer> unionSet, int psica) {
        this.unionSet = unionSet;
        this.psica = psica;
    }

    /**
     * Gets the union.
     *
     * @return union.
     */
    public Set<ByteBuffer> getUnion() {
        return unionSet;
    }

    /**
     * Gets PSI-CA.
     *
     * @return PSI-CA.
     */
    public int getPsica() {
        return psica;
    }
}

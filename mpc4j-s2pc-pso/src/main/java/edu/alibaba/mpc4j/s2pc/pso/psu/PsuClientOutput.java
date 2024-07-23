package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * PSU client output.
 *
 * @author Weiran Liu
 * @date 2024/5/4
 */
public class PsuClientOutput {
    /**
     * union set
     */
    private final Set<ByteBuffer> union;
    /**
     * PSI-CA
     */
    private final int psica;

    public PsuClientOutput(Set<ByteBuffer> union, int psica) {
        MathPreconditions.checkNonNegativeInRangeClosed("PSI-CA", psica, union.size());
        this.union = union;
        this.psica = psica;
    }

    /**
     * Gets union.
     *
     * @return union.
     */
    public Set<ByteBuffer> getUnion() {
        return union;
    }

    /**
     * Gets PSI-CA.
     *
     * @return PSI-CA.
     */
    public int getPsiCa() {
        return psica;
    }
}

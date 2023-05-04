package edu.alibaba.mpc4j.s2pc.pcg.ot;

import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

/**
 * Oblivious transfer sender output interface.
 *
 * @author Weiran Liu
 * @date 2022/12/21
 */
public interface OtSenderOutput extends PcgPartyOutput {
    /**
     * Get the value of R0 at the index.
     *
     * @param index the index.
     * @return R0.
     */
    byte[] getR0(int index);

    /**
     * Get the R0 array.
     *
     * @return the R0 array.
     */
    byte[][] getR0Array();

    /**
     * Get the value of R1 at the index.
     *
     * @param index the index.
     * @return R1.
     */
    byte[] getR1(int index);

    /**
     * Get the R1 array.
     *
     * @return the R1 array.
     */
    byte[][] getR1Array();
}

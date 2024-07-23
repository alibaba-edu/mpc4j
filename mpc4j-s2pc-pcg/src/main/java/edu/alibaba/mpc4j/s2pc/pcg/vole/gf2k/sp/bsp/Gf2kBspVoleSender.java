package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * Batched single-point GF2K-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/7/12
 */
public interface Gf2kBspVoleSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param subfieldL subfield L.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int subfieldL) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param alphaArray α array.
     * @param eachNum    each num.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kBspVoleSenderOutput send(int[] alphaArray, int eachNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param alphaArray      α array.
     * @param eachNum         each num.
     * @param preSenderOutput pre-computed sender output.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kBspVoleSenderOutput send(int[] alphaArray, int eachNum, Gf2kVoleSenderOutput preSenderOutput)
        throws MpcAbortException;
}
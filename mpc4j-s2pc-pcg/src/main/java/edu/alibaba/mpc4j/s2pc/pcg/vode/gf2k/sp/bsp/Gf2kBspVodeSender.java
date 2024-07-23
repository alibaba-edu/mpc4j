package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;

/**
 * GF2K-BSP-VODE sender.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public interface Gf2kBspVodeSender extends TwoPartyPto {
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
    Gf2kBspVodeSenderOutput send(int[] alphaArray, int eachNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param alphaArray      α array.
     * @param eachNum         each num.
     * @param preSenderOutput pre-computed sender output.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kBspVodeSenderOutput send(int[] alphaArray, int eachNum, Gf2kVodeSenderOutput preSenderOutput)
        throws MpcAbortException;
}

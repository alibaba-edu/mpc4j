package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.Gf2kVodeSenderOutput;

/**
 * GF2K-MSP-VODE sender.
 *
 * @author Weiran Liu
 * @date 2024/6/12
 */
public interface Gf2kMspVodeSender extends TwoPartyPto {
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
     * @param t   sparse num.
     * @param num num.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kMspVodeSenderOutput send(int t, int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param t               sparse num.
     * @param num             num.
     * @param preSenderOutput pre-computed sender output.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kMspVodeSenderOutput send(int t, int num, Gf2kVodeSenderOutput preSenderOutput) throws MpcAbortException;
}

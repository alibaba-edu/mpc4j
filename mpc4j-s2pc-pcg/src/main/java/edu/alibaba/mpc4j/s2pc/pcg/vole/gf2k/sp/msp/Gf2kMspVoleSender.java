package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.msp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * multi single-point GF2K-VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public interface Gf2kMspVoleSender extends TwoPartyPto {
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
    Gf2kMspVoleSenderOutput send(int t, int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param t               sparse num.
     * @param num             num.
     * @param preSenderOutput pre-computed sender output.
     * @return sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kMspVoleSenderOutput send(int t, int num, Gf2kVoleSenderOutput preSenderOutput) throws MpcAbortException;
}

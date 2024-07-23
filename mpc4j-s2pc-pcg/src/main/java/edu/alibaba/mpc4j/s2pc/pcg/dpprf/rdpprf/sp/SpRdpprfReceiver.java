package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * single-point RDPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public interface SpRdpprfReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException if the protocol aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param alpha α.
     * @param num   n.
     * @return receiver output.
     * @throws MpcAbortException if the protocol aborts.
     */
    SpRdpprfReceiverOutput puncture(int alpha, int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param alpha             α.
     * @param num               num.
     * @param preReceiverOutput pre-computed COT receiver output.
     * @return receiver output.
     * @throws MpcAbortException if the protocol aborts.
     */
    SpRdpprfReceiverOutput puncture(int alpha, int num, CotReceiverOutput preReceiverOutput) throws MpcAbortException;
}

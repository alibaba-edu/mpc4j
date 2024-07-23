package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * single-point CDPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public interface SpCdpprfReceiver extends TwoPartyPto {
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
    SpCdpprfReceiverOutput puncture(int alpha, int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param alpha             α.
     * @param num               num.
     * @param preReceiverOutput pre-computed COT receiver output.
     * @return receiver output.
     * @throws MpcAbortException if the protocol aborts.
     */
    SpCdpprfReceiverOutput puncture(int alpha, int num, CotReceiverOutput preReceiverOutput) throws MpcAbortException;
}

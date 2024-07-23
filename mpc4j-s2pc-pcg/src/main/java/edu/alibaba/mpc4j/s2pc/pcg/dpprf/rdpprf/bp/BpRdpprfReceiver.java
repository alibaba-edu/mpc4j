package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * batch-point RDPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public interface BpRdpprfReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException if the protocol aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param alphaArray α array.
     * @param eachNum    each num.
     * @return receiver output.
     * @throws MpcAbortException if the protocol aborts.
     */
    BpRdpprfReceiverOutput puncture(int[] alphaArray, int eachNum) throws MpcAbortException;

    /**
     * Execute the protocol.
     *
     * @param alphaArray        α array.
     * @param eachNum           each num.
     * @param preReceiverOutput pre-computed COT receiver output.
     * @return receiver output.
     * @throws MpcAbortException if the protocol aborts.
     */
    BpRdpprfReceiverOutput puncture(int[] alphaArray, int eachNum, CotReceiverOutput preReceiverOutput) throws MpcAbortException;
}

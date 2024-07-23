package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * BP-CDPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public interface BpCdpprfReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param alphaArray α array.
     * @param eachNum    each num.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    BpCdpprfReceiverOutput puncture(int[] alphaArray, int eachNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param alphaArray        α array.
     * @param eachNum           each num.
     * @param preReceiverOutput pre-computed COT receiver output.
     * @return receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    BpCdpprfReceiverOutput puncture(int[] alphaArray, int eachNum, CotReceiverOutput preReceiverOutput)
        throws MpcAbortException;
}

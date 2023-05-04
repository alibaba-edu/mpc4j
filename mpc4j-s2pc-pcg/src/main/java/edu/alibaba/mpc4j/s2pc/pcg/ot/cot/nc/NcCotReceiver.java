package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * no-choice COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface NcCotReceiver extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param num num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @return the receiver output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    CotReceiverOutput receive() throws MpcAbortException;
}

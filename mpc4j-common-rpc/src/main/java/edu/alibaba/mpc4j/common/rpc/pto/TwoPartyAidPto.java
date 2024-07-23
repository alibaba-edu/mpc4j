package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * two party aid protocol.
 *
 * @author Weiran Liu
 * @date 2024/6/10
 */
public interface TwoPartyAidPto extends MultiPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init() throws MpcAbortException;
    /**
     * aid.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void aid() throws MpcAbortException;
}

package edu.alibaba.mpc4j.s2pc.pcg.ct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * coin-tossing protocol party.
 *
 * @author Weiran Liu
 * @date 2023/5/4
 */
public interface CoinTossParty extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init() throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param num       num.
     * @param bitLength bit length for each coin.
     * @return coin-tossing result.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[][] coinToss(int num, int bitLength) throws MpcAbortException;
}

package edu.alibaba.mpc4j.common.rpc.pto;

import edu.alibaba.mpc4j.common.rpc.Party;

/**
 * Three-party protocol.
 *
 * @author Weiran Liu
 * @date 2021/12/19
 */
public interface ThreePartyPto extends MultiPartyPto {
    /**
     * Gets left party.
     *
     * @return left party.
     */
    default Party leftParty() {
        return otherParties()[0];
    }

    /**
     * Gets right party.
     *
     * @return right party.
     */
    default Party rightParty() {
        return otherParties()[1];
    }
}

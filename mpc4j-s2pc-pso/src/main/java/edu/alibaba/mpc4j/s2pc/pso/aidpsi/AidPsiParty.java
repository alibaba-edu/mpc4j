package edu.alibaba.mpc4j.s2pc.pso.aidpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.ThreePartyPto;

import java.util.Set;

/**
 * aided PSI party.
 *
 * @author Weiran Liu
 * @date 2023/5/4
 */
public interface AidPsiParty<T> extends ThreePartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxOwnElementSize   max own element size.
     * @param maxOtherElementSize max other element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxOwnElementSize, int maxOtherElementSize) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param ownElementSet    own element set.
     * @param otherElementSize other element size.
     * @return intersection.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Set<T> psi(Set<T> ownElementSet, int otherElementSize) throws MpcAbortException;
}

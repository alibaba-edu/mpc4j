package edu.alibaba.mpc4j.s2pc.pso.aidpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pso.aidpsi.AidPsiParty;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * aid PSI party thread.
 *
 * @author Weiran Liu
 * @date 2023/5/8
 */
class AidPsiPartyThread extends Thread {
    /**
     * aid PSI party
     */
    private final AidPsiParty<ByteBuffer> party;
    /**
     * own element set
     */
    private final Set<ByteBuffer> ownElementSet;
    /**
     * other element size
     */
    private final int otherElementSize;
    /**
     * intersection set
     */
    private Set<ByteBuffer> intersectionSet;

    AidPsiPartyThread(AidPsiParty<ByteBuffer> party, Set<ByteBuffer> ownElementSet, int otherElementSize) {
        this.party = party;
        this.ownElementSet = ownElementSet;
        this.otherElementSize = otherElementSize;
    }

    Set<ByteBuffer> getIntersectionSet() {
        return intersectionSet;
    }

    @Override
    public void run() {
        try {
            party.init(ownElementSet.size(), otherElementSize);
            intersectionSet = party.psi(ownElementSet, otherElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

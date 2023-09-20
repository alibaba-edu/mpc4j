package edu.alibaba.mpc4j.s2pc.pjc.pid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Set;

/**
 * PID thread.
 *
 * @author Weiran Liu
 * @date 2022/01/21
 */
class PidPartyThread extends Thread {
    /**
     * party
     */
    private final PidParty<String> pidParty;
    /**
     * own element set
     */
    private final Set<String> ownElementSet;
    /**
     * other set size
     */
    private final int otherSetSize;
    /**
     * PID
     */
    private PidPartyOutput<String> pidPartyOutput;

    PidPartyThread(PidParty<String> pidParty, Set<String> ownElementSet, int otherSetSize) {
        this.pidParty = pidParty;
        this.ownElementSet = ownElementSet;
        this.otherSetSize = otherSetSize;
    }

    PidPartyOutput<String> getPidOutput() {
        return pidPartyOutput;
    }

    @Override
    public void run() {
        try {
            pidParty.init(ownElementSet.size(), otherSetSize);
            pidPartyOutput = pidParty.pid(ownElementSet, otherSetSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
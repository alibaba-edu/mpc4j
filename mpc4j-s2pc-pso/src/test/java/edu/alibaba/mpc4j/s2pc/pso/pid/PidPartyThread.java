package edu.alibaba.mpc4j.s2pc.pso.pid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Set;

/**
 * PID协议参与方线程。
 *
 * @author Weiran Liu
 * @date 2022/01/21
 */
class PidPartyThread extends Thread {
    /**
     * 服务端
     */
    private final PidParty<String> pidParty;
    /**
     * 自己元素集合
     */
    private final Set<String> ownElementSet;
    /**
     * 对方元素数量
     */
    private final int otherSetSize;
    /**
     * PID输出结果
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
            pidParty.getRpc().connect();
            pidParty.init(ownElementSet.size(), otherSetSize);
            pidPartyOutput = pidParty.pid(ownElementSet, otherSetSize);
            pidParty.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
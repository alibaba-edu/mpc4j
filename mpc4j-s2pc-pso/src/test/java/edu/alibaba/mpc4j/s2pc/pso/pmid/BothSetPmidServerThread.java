package edu.alibaba.mpc4j.s2pc.pso.pmid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Set;

/**
 * 双方集合PMID服务端线程。
 *
 * @author Weiran Liu
 * @date 2022/08/26
 */
class BothSetPmidServerThread extends Thread {
    /**
     * PMID服务端
     */
    private final PmidServer<String> pmidServer;
    /**
     * 服务端集合
     */
    private final Set<String> serverElementSet;
    /**
     * 客户端元素数量
     */
    private final int clientSetSize;
    /**
     * PMID输出结果
     */
    private PmidPartyOutput<String> serverOutput;

    BothSetPmidServerThread(PmidServer<String> pmidServer, Set<String> serverElementSet, int clientSetSize) {
        this.pmidServer = pmidServer;
        this.serverElementSet = serverElementSet;
        this.clientSetSize = clientSetSize;
    }

    PmidPartyOutput<String> getServerOutput() {
        return serverOutput;
    }

    @Override
    public void run() {
        try {
            pmidServer.getRpc().connect();
            pmidServer.init(serverElementSet.size(), 1, clientSetSize, 1);
            serverOutput = pmidServer.pmid(serverElementSet, clientSetSize);
            pmidServer.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

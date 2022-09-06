package edu.alibaba.mpc4j.s2pc.pso.pmid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Set;

/**
 * 双方集合PMID客户端线程。
 *
 * @author Weiran Liu
 * @date 2022/08/26
 */
class BothSetPmidClientThread extends Thread {
    /**
     * PMID客户端
     */
    private final PmidClient<String> pmidClient;
    /**
     * 客户端集合
     */
    private final Set<String> clientElementSet;
    /**
     * 服务端集合数量
     */
    private final int serverSetSize;
    /**
     * PMID输出结果
     */
    private PmidPartyOutput<String> clientOutput;

    BothSetPmidClientThread(PmidClient<String> pmidClient, Set<String> clientElementSet, int serverSetSize) {
        this.pmidClient = pmidClient;
        this.clientElementSet = clientElementSet;
        this.serverSetSize = serverSetSize;
    }

    PmidPartyOutput<String> getClientOutput() {
        return clientOutput;
    }

    @Override
    public void run() {
        try {
            pmidClient.getRpc().connect();
            pmidClient.init(clientElementSet.size(), 1, serverSetSize, 1);
            clientOutput = pmidClient.pmid(clientElementSet, serverSetSize);
            pmidClient.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

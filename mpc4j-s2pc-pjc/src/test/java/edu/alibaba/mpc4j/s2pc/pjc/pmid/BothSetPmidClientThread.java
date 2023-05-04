package edu.alibaba.mpc4j.s2pc.pjc.pmid;

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
    private final PmidClient<String> client;
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

    BothSetPmidClientThread(PmidClient<String> client, Set<String> clientElementSet, int serverSetSize) {
        this.client = client;
        this.clientElementSet = clientElementSet;
        this.serverSetSize = serverSetSize;
    }

    PmidPartyOutput<String> getClientOutput() {
        return clientOutput;
    }

    @Override
    public void run() {
        try {
            client.init(clientElementSet.size(), 1, serverSetSize, 1);
            clientOutput = client.pmid(clientElementSet, serverSetSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

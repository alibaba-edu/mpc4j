package edu.alibaba.mpc4j.s2pc.pjc.pmid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Set;

/**
 * 服务端集合PMID协议服务端线程。
 *
 * @author Weiran Liu
 * @date 2022/05/10
 */
class ServerSetPmidServerThread extends Thread {
    /**
     * PMID服务端
     */
    private final PmidServer<String> server;
    /**
     * 服务端集合
     */
    private final Set<String> serverElementSet;
    /**
     * 客户端元素数量
     */
    private final int clientSetSize;
    /**
     * 客户端最大重数上界
     */
    private final int maxClientU;
    /**
     * 客户端重数上界
     */
    private final int clientU;
    /**
     * PMID输出结果
     */
    private PmidPartyOutput<String> serverOutput;

    ServerSetPmidServerThread(PmidServer<String> server,
                              Set<String> serverElementSet, int clientSetSize, int maxClientU, int ClientU) {
        this.server = server;
        this.serverElementSet = serverElementSet;
        this.clientSetSize = clientSetSize;
        this.maxClientU = maxClientU;
        this.clientU = ClientU;
    }

    PmidPartyOutput<String> getServerOutput() {
        return serverOutput;
    }

    @Override
    public void run() {
        try {
            server.init(serverElementSet.size(), 1, clientSetSize, maxClientU);
            serverOutput = server.pmid(serverElementSet, clientSetSize, clientU);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

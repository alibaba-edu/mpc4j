package edu.alibaba.mpc4j.s2pc.pso.pmid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Map;

/**
 * PMID协议服务端线程。
 *
 * @author Weiran Liu
 * @date 2022/08/26
 */
class PmidServerThread extends Thread {
    /**
     * PMID服务端
     */
    private final PmidServer<String> pmidServer;
    /**
     * 服务端集合
     */
    private final Map<String, Integer> serverElementMap;
    /**
     * 服务端最大重数上界
     */
    private final int maxServerU;
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

    PmidServerThread(PmidServer<String> pmidServer,
                     Map<String, Integer> serverElementMap, int maxServerU,
                     int clientSetSize, int maxClientU, int clientU) {
        this.pmidServer = pmidServer;
        this.serverElementMap = serverElementMap;
        this.maxServerU = maxServerU;
        this.clientSetSize = clientSetSize;
        this.maxClientU = maxClientU;
        this.clientU = clientU;
    }

    PmidPartyOutput<String> getServerOutput() {
        return serverOutput;
    }

    @Override
    public void run() {
        try {
            pmidServer.getRpc().connect();
            pmidServer.init(serverElementMap.size(), maxServerU, clientSetSize, maxClientU);
            serverOutput = pmidServer.pmid(serverElementMap, clientSetSize, clientU);
            pmidServer.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

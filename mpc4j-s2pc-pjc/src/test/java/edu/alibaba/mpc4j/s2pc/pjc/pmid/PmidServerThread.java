package edu.alibaba.mpc4j.s2pc.pjc.pmid;

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
    private final PmidServer<String> server;
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

    PmidServerThread(PmidServer<String> server,
                     Map<String, Integer> serverElementMap, int maxServerU,
                     int clientSetSize, int maxClientU, int clientU) {
        this.server = server;
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
            server.init(serverElementMap.size(), maxServerU, clientSetSize, maxClientU);
            serverOutput = server.pmid(serverElementMap, clientSetSize, clientU);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

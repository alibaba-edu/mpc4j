package edu.alibaba.mpc4j.s2pc.pso.pmid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Map;

/**
 * PMID协议客户端线程。
 *
 * @author Weiran Liu
 * @date 2022/08/26
 */
public class PmidClientThread extends Thread {
    /**
     * PMID客户端
     */
    private final PmidClient<String> pmidClient;
    /**
     * 客户端映射
     */
    private final Map<String, Integer> clientElementMap;
    /**
     * 客户端重数上界
     */
    private final int maxClientU;
    /**
     * 服务端集合数量
     */
    private final int serverSetSize;
    /**
     * 服务端最大重数上界
     */
    private final int maxServerU;
    /**
     * 服务端重数上界
     */
    private final int serverU;
    /**
     * PMID输出结果
     */
    private PmidPartyOutput<String> clientOutput;

    PmidClientThread(PmidClient<String> pmidClient,
                     Map<String, Integer> clientElementMap, int maxClientU,
                     int serverSetSize, int maxServerU, int serverU) {
        this.pmidClient = pmidClient;
        this.clientElementMap = clientElementMap;
        this.maxClientU = maxClientU;
        this.serverSetSize = serverSetSize;
        this.maxServerU = maxServerU;
        this.serverU = serverU;
    }

    PmidPartyOutput<String> getClientOutput() {
        return clientOutput;
    }

    @Override
    public void run() {
        try {
            pmidClient.getRpc().connect();
            pmidClient.init(clientElementMap.keySet().size(), maxClientU, serverSetSize, maxServerU);
            clientOutput = pmidClient.pmid(clientElementMap, serverSetSize, serverU);
            pmidClient.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

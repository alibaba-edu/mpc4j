package edu.alibaba.mpc4j.s2pc.pso.pmid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.util.Map;
import java.util.Set;

/**
 * 客户端集合PMID协议客户端线程。
 *
 * @author Weiran Liu
 * @date 2022/08/26
 */
public class ClientSetPmidClientThread extends Thread {
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

    ClientSetPmidClientThread(PmidClient<String> pmidClient,
                              Set<String> clientElementSet, int serverSetSize, int maxServerU, int serverU) {
        this.pmidClient = pmidClient;
        this.clientElementSet = clientElementSet;
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
            pmidClient.init(clientElementSet.size(), 1, serverSetSize, maxServerU);
            clientOutput = pmidClient.pmid(clientElementSet, serverSetSize, serverU);
            pmidClient.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

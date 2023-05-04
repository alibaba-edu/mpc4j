package edu.alibaba.mpc4j.s2pc.pjc.pmid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

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

    ClientSetPmidClientThread(PmidClient<String> client,
                              Set<String> clientElementSet, int serverSetSize, int maxServerU, int serverU) {
        this.client = client;
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
            client.init(clientElementSet.size(), 1, serverSetSize, maxServerU);
            clientOutput = client.pmid(clientElementSet, serverSetSize, serverU);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

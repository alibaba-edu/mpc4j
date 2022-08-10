package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * 关键词索引PIR协议客户端线程。
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
public class KwPirClientThread<T> extends Thread {
    /**
     * 关键词PIR协议客户端
     */
    private final KwPirClient<T> client;
    /**
     * 关键字PIR参数
     */
    private final KwPirParams kwPirParams;
    /**
     * 标签字节长度
     */
    private final int labelByteLength;
    /**
     * 检索集合
     */
    private final ArrayList<Set<T>> retrievalSets;
    /**
     * 检索次数
     */
    private final int repeatTime;
    /**
     * PIR结果
     */
    private final ArrayList<Map<T, ByteBuffer>> retrievalResults;

    KwPirClientThread(KwPirClient<T> client, KwPirParams kwPirParams, ArrayList<Set<T>> retrievalSets, int labelByteLength) {
        this.client = client;
        this.kwPirParams = kwPirParams;
        this.retrievalSets = retrievalSets;
        this.labelByteLength = labelByteLength;
        repeatTime = retrievalSets.size();
        retrievalResults = new ArrayList<>(repeatTime);
    }

    public Map<T, ByteBuffer> getRetrievalResult(int index) {
        return retrievalResults.get(index);
    }

    @Override
    public void run() {
        try {
            // 随机选取
            client.getRpc().connect();
            client.init(kwPirParams, labelByteLength);
            client.getRpc().synchronize();
            for (int i = 0; i < repeatTime; i++) {
                retrievalResults.add(client.pir(retrievalSets.get(i)));
            }
            client.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
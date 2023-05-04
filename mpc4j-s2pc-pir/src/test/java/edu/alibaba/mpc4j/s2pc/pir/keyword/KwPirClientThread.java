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
     * 标签字节长度
     */
    private final int labelByteLength;
    /**
     * 检索集合
     */
    private final ArrayList<Set<T>> retrievalSets;
    /**
     * 客户端检索数量
     */
    private final int retrievalSize;
    /**
     * 检索次数
     */
    private final int repeatTime;
    /**
     * PIR结果
     */
    private final ArrayList<Map<T, ByteBuffer>> retrievalResults;

    KwPirClientThread(KwPirClient<T> client, ArrayList<Set<T>> retrievalSets, int retrievalSize, int labelByteLength) {
        this.client = client;
        this.retrievalSets = retrievalSets;
        this.retrievalSize = retrievalSize;
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
            client.init(retrievalSize, labelByteLength);
            client.getRpc().synchronize();
            for (int i = 0; i < repeatTime; i++) {
                retrievalResults.add(client.pir(retrievalSets.get(i)));
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
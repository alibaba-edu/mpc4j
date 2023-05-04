package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * 关键词索引PIR协议服务端线程。
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
public class KwPirServerThread<T> extends Thread {
    /**
     * 服务端
     */
    private final KwPirServer<T> server;
    /**
     * 关键词标签映射
     */
    private final Map<T, ByteBuffer> keywordLabelMap;
    /**
     * 客户端检索数量
     */
    private final int retrievalSize;
    /**
     * 标签字节长度
     */
    private final int labelByteLength;
    /**
     * 重复次数
     */
    private final int repeatTime;

    KwPirServerThread(KwPirServer<T> server, Map<T, ByteBuffer> keywordLabelMap, int retrievalSize, int labelByteLength, int repeatTime) {
        this.server = server;
        this.keywordLabelMap = keywordLabelMap;
        this.retrievalSize = retrievalSize;
        this.labelByteLength = labelByteLength;
        this.repeatTime = repeatTime;
    }

    @Override
    public void run() {
        try {
            server.init(keywordLabelMap, retrievalSize, labelByteLength);
            server.getRpc().synchronize();
            for (int i = 0; i < repeatTime; i++) {
                server.pir();
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
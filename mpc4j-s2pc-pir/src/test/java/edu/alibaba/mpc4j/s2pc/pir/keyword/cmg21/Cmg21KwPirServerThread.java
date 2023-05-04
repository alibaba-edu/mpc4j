package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * CMG21关键词PIR协议服务端线程。
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
public class Cmg21KwPirServerThread<T> extends Thread {
    /**
     * CMG21关键词PIR协议服务端
     */
    private final Cmg21KwPirServer<T> server;
    /**
     * CMG21关键词PIR协议配置项
     */
    private final Cmg21KwPirParams kwPirParams;
    /**
     * 关键词标签映射
     */
    private final Map<T, ByteBuffer> keywordLabelMap;
    /**
     * 标签字节长度
     */
    private final int labelByteLength;
    /**
     * 重复次数
     */
    private final int repeatTime;

    Cmg21KwPirServerThread(Cmg21KwPirServer<T> server, Cmg21KwPirParams kwPirParams, Map<T, ByteBuffer> keywordLabelMap,
                           int labelByteLength, int repeatTime) {
        this.server = server;
        this.kwPirParams = kwPirParams;
        this.keywordLabelMap = keywordLabelMap;
        this.labelByteLength = labelByteLength;
        this.repeatTime = repeatTime;
    }

    @Override
    public void run() {
        try {
            server.init(kwPirParams, keywordLabelMap, labelByteLength);
            server.getRpc().synchronize();
            for (int i = 0; i < repeatTime; i++) {
                server.pir();
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
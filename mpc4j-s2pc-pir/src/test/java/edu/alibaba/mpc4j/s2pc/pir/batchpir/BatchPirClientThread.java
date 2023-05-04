package edu.alibaba.mpc4j.s2pc.pir.batchpir;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量索引PIR协议客户端线程。
 *
 * @author Liqiang Peng
 * @date 2023/3/9
 */
public class BatchPirClientThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchPirClientThread.class);
    /**
     * 客户端
     */
    private final BatchIndexPirClient client;
    /**
     * 客户端索引值列表
     */
    private final List<Integer> retrievalIndexList;
    /**
     * 检索结果映射
     */
    private Map<Integer, byte[]> retrievalResults;
    /**
     * 元素比特长度
     */
    private final int elementBitLength;
    /**
     * 服务端元素数量
     */
    private final int serverElementSize;
    /**
     * 支持的最大查询数目
     */
    private final int maxRetrievalSize;

    BatchPirClientThread(BatchIndexPirClient client, List<Integer> retrievalIndexList, int elementBitLength,
                         int serverElementSize, int maxRetrievalSize) {
        this.client = client;
        this.retrievalIndexList = retrievalIndexList;
        this.retrievalResults = new HashMap<>(retrievalIndexList.size());
        this.elementBitLength = elementBitLength;
        this.serverElementSize = serverElementSize;
        this.maxRetrievalSize = maxRetrievalSize;
    }

    public Map<Integer, byte[]> getRetrievalResult() {
        return retrievalResults;
    }

    @Override
    public void run() {
        try {
            client.init(serverElementSize, elementBitLength, maxRetrievalSize);
            LOGGER.info(
                "Client: Offline Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();
            retrievalResults = client.pir(retrievalIndexList);
            LOGGER.info(
                "Client: Online Communication costs {}MB", client.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            client.getRpc().synchronize();
            client.getRpc().reset();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
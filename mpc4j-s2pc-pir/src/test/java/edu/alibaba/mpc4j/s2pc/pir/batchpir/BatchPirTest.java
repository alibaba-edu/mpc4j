package edu.alibaba.mpc4j.s2pc.pir.batchpir;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.BatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.psipir.Lpzg24BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.batchindex.vectorizedpir.Mr23BatchIndexPirConfig;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * 批量索引PIR测试类。
 *
 * @author Liqiang Peng
 * @date 2023/3/9
 */
@RunWith(Parameterized.class)
public class BatchPirTest {
    /**
     * 默认比特长度
     */
    private static final int DEFAULT_BIT_LENGTH = 16;
    /**
     * 较小比特长度
     */
    private static final int SMALL_BIT_LENGTH = 1;
    /**
     * 较大比特长度
     */
    private static final int LARGE_BIT_LENGTH = 32;
    /**
     * 较小服务端元素数量
     */
    private static final int SMALL_SERVER_ELEMENT_SIZE = 1 << 14;
    /**
     * 默认服务端元素数量
     */
    private static final int DEFAULT_SERVER_ELEMENT_SIZE = 1 << 16;
    /**
     * 默认检索数目
     */
    private static final int DEFAULT_RETRIEVAL_SIZE = 1 << 11;
    /**
     * 特殊检索数目
     */
    private static final int SPECIAL_RETRIEVAL_SIZE = 1 << 8 + 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // PSI-PIR
        configurations.add(new Object[]{
            BatchIndexPirFactory.BatchIndexPirType.PSI_PIR.name(),
            new Lpzg24BatchIndexPirConfig.Builder().build()
        });
        // vectorized batch PIR
        configurations.add(new Object[]{
            BatchIndexPirFactory.BatchIndexPirType.VECTORIZED_BATCH_PIR.name(),
            new Mr23BatchIndexPirConfig.Builder().build()
        });
        return configurations;
    }

    /**
     * 服务端
     */
    private final Rpc serverRpc;
    /**
     * 客户端
     */
    private final Rpc clientRpc;
    /**
     * 批量索引PIR配置项
     */
    private final BatchIndexPirConfig config;

    public BatchPirTest(String name, BatchIndexPirConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testSmallBitLength() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, DEFAULT_RETRIEVAL_SIZE, SMALL_BIT_LENGTH, false);
    }

    @Test
    public void testDefaultBitLengthParallel() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, DEFAULT_RETRIEVAL_SIZE, DEFAULT_BIT_LENGTH, true);
    }

    @Test
    public void testDefaultBitLength() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, DEFAULT_RETRIEVAL_SIZE, DEFAULT_BIT_LENGTH, false);
    }

    @Test
    public void testLargeBitLength() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, DEFAULT_RETRIEVAL_SIZE, LARGE_BIT_LENGTH, true);
    }

    @Test
    public void test2Retrieval() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, 2, DEFAULT_BIT_LENGTH, false);
    }

    @Test
    public void test1Retrieval() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, 1, DEFAULT_BIT_LENGTH, false);
    }

    @Test
    public void testSmallElementSize() {
        testPto(SMALL_SERVER_ELEMENT_SIZE, DEFAULT_RETRIEVAL_SIZE, DEFAULT_BIT_LENGTH, false);
    }

    @Test
    public void testSpecialElementSize() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE + 1, DEFAULT_RETRIEVAL_SIZE, DEFAULT_BIT_LENGTH, false);
    }

    @Test
    public void testSpecialRetrievalSize() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, SPECIAL_RETRIEVAL_SIZE, DEFAULT_BIT_LENGTH, false);
    }

    @Test
    public void testSmallElementSize1Retrieval() {
        testPto(SMALL_SERVER_ELEMENT_SIZE, 1, DEFAULT_BIT_LENGTH, false);
    }

    @Test
    public void testSmallElementSize2Retrieval() {
        testPto(SMALL_SERVER_ELEMENT_SIZE, 2, DEFAULT_BIT_LENGTH, false);
    }

    public void testPto(int serverElementSize, int retrievalIndexSize, int elementBitLength, boolean parallel) {
        Set<Integer> retrievalIndexSet = PirUtils.generateRetrievalIndexSet(serverElementSize, retrievalIndexSize);
        NaiveDatabase database = PirUtils.generateDataBase(serverElementSize, elementBitLength);
        // 创建参与方实例
        BatchIndexPirServer server = BatchIndexPirFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        BatchIndexPirClient client = BatchIndexPirFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        BatchPirServerThread serverThread = new BatchPirServerThread(server, database, retrievalIndexSize);
        List<Integer> retrievalIndexList = new ArrayList<>(retrievalIndexSet);
        BatchPirClientThread clientThread = new BatchPirClientThread(
            client, retrievalIndexList, elementBitLength, serverElementSize, retrievalIndexSize
        );
        try {
            // 开始执行协议
            serverThread.start();
            clientThread.start();
            // 等待线程停止
            serverThread.join();
            clientThread.join();
            // 验证结果
            Map<Integer, byte[]> result = clientThread.getRetrievalResult();
            Assert.assertEquals(retrievalIndexSize, result.size());
            result.forEach((key, value) ->
                Assert.assertEquals(ByteBuffer.wrap(database.getBytesData(key)), ByteBuffer.wrap(value))
            );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }
}



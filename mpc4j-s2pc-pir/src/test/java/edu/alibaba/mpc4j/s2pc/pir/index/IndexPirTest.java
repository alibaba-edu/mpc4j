package edu.alibaba.mpc4j.s2pc.pir.index;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.Mbfk16IndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.Mbfk16IndexPirParams;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 索引PIR测试类。
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
@RunWith(Parameterized.class)
public class IndexPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexPirTest.class);
    /**
     * 重复检索次数
     */
    private static final int REPEAT_TIME = 5;
    /**
     * 短标签字节长度
     */
    private static final int SHORT_ELEMENT_BYTE_LENGTH = 1;
    /**
     * 默认标签字节长度
     */
    private static final int DEFAULT_ELEMENT_BYTE_LENGTH = 16;
    /**
     * 长标签字节长度
     */
    private static final int LARGE_ELEMENT_BYTE_LENGTH = 33;
    /**
     * 服务端元素数量
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 20;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // XPIR
        Mbfk16IndexPirConfig xpirConfig = new Mbfk16IndexPirConfig.Builder()
            .setPolyModulusDegree(4096)
            .setPlainModulusSize(20)
            .build();
        // XPIR (1-dimension)
        configurations.add(new Object[] {
            IndexPirFactory.IndexPirType.XPIR.name() + " (1-dimension)",
            xpirConfig,
            new Mbfk16IndexPirParams(
                SERVER_ELEMENT_SIZE, LARGE_ELEMENT_BYTE_LENGTH, xpirConfig, 1
            )
        });
        // XPIR (2-dimension)
        configurations.add(new Object[] {
            IndexPirFactory.IndexPirType.XPIR.name() + " (2-dimension)",
            xpirConfig,
            new Mbfk16IndexPirParams(
                SERVER_ELEMENT_SIZE, LARGE_ELEMENT_BYTE_LENGTH, xpirConfig, 2
            )
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
     * 索引PIR配置项
     */
    private final IndexPirConfig indexPirConfig;
    /**
     * 索引PIR参数
     */
    private final IndexPirParams indexPirParams;

    public IndexPirTest(String name, IndexPirConfig indexPirConfig, IndexPirParams indexPirParams) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.indexPirConfig = indexPirConfig;
        this.indexPirParams = indexPirParams;
    }

    @Test
    public void testShortElements() {
        testIndexPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testDefaultElements() {
        testIndexPir(indexPirConfig, indexPirParams, SHORT_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testLargeElements() {
        testIndexPir(indexPirConfig, indexPirParams, LARGE_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testParallel() {
        testIndexPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, true);
    }

    public void testIndexPir(IndexPirConfig config, IndexPirParams indexPirParams, int elementByteLength, boolean parallel) {
        ArrayList<Integer> retrievalIndexList = PirUtils.generateRetrievalIndexList(SERVER_ELEMENT_SIZE, REPEAT_TIME);
        // 生成元素数组
        ArrayList<ByteBuffer> elementList = PirUtils.generateElementArrayList(SERVER_ELEMENT_SIZE, elementByteLength);
        // 创建参与方实例
        IndexPirServer server = IndexPirFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        IndexPirClient client = IndexPirFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        IndexPirServerThread serverThread = new IndexPirServerThread(
            server, indexPirParams, elementList, elementByteLength, REPEAT_TIME
        );
        IndexPirClientThread clientThread = new IndexPirClientThread(
            client, indexPirParams, retrievalIndexList, SERVER_ELEMENT_SIZE, elementByteLength, REPEAT_TIME
        );
        try {
            // 开始执行协议
            serverThread.start();
            clientThread.start();
            // 等待线程停止
            serverThread.join();
            clientThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LOGGER.info("Server: The Communication costs {}MB", serverRpc.getSendByteLength() * 1.0 / (1024 * 1024));
        serverRpc.reset();
        LOGGER.info("Client: The Communication costs {}MB", clientRpc.getSendByteLength() * 1.0 / (1024 * 1024));
        clientRpc.reset();
        // 验证结果
        ArrayList<ByteBuffer> result = clientThread.getRetrievalResult();
        LOGGER.info("Client: The Retrieval Set Size is {}", result.size());
        for (int index = 0; index < REPEAT_TIME; index++) {
            ByteBuffer retrievalElement = result.get(index);
            Assert.assertEquals(retrievalElement, elementList.get(retrievalIndexList.get(index)));
        }
    }
}
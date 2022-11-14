package edu.alibaba.mpc4j.s2pc.pir.keyword;

import com.google.common.collect.Lists;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirParams;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * 关键词索引PIR测试类。
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
public class KwPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(KwPirTest.class);
    /**
     * 重复检索次数
     */
    private static final int REPEAT_TIME = 1;
    /**
     * 短标签字节长度
     */
    private static final int SHORT_LABEL_BYTE_LENGTH = 1;
    /**
     * 默认标签字节长度
     */
    private static final int DEFAULT_LABEL_BYTE_LENGTH = 32;
    /**
     * 长标签字节长度
     */
    private static final int LONG_LABEL_BYTE_LENGTH = 65;
    /**
     * 服务端元素数量
     */
    private static final int SERVER_MAP_SIZE = 1 << 16;
    /**
     * 服务端
     */
    private final Rpc serverRpc;
    /**
     * 客户端
     */
    private final Rpc clientRpc;

    public KwPirTest() {
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
    }

    @Test
    public void test1M1() {
        Cmg21KwPirConfig config = new Cmg21KwPirConfig.Builder().build();
        testPir(config, Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_1, DEFAULT_LABEL_BYTE_LENGTH, false);
    }

    @Test
    public void test1M1Parallel() {
        Cmg21KwPirConfig config = new Cmg21KwPirConfig.Builder().build();
        testPir(config, Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_1, DEFAULT_LABEL_BYTE_LENGTH, true);
    }

    @Test
    public void test1M1ShortLabelParallel() {
        Cmg21KwPirConfig config = new Cmg21KwPirConfig.Builder().build();
        testPir(config, Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_1, SHORT_LABEL_BYTE_LENGTH, true);
    }

    @Test
    public void test1M1LongLabelParallel() {
        Cmg21KwPirConfig config = new Cmg21KwPirConfig.Builder().build();
        testPir(config, Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_1, LONG_LABEL_BYTE_LENGTH, true);
    }

    @Test
    public void test1M4096() {
        Cmg21KwPirConfig config = new Cmg21KwPirConfig.Builder().build();
        testPir(config, Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_4096, DEFAULT_LABEL_BYTE_LENGTH, false);
    }

    @Test
    public void test1M4096Parallel() {
        Cmg21KwPirConfig config = new Cmg21KwPirConfig.Builder().build();
        testPir(config, Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_4096, DEFAULT_LABEL_BYTE_LENGTH, true);
    }

    @Test
    public void test1M4096ShortLabelParallel() {
        Cmg21KwPirConfig config = new Cmg21KwPirConfig.Builder().build();
        testPir(config, Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_4096, SHORT_LABEL_BYTE_LENGTH, true);
    }

    @Test
    public void test1M4096LongLabelParallel() {
        Cmg21KwPirConfig config = new Cmg21KwPirConfig.Builder().build();
        testPir(config, Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_4096, LONG_LABEL_BYTE_LENGTH, true);
    }

    public void testPir(Cmg21KwPirConfig config, KwPirParams kwPirParams, int labelByteLength, boolean parallel) {
        int retrievalSize = kwPirParams.maxRetrievalSize();
        ArrayList<Set<String>> randomSets = PirUtils.generateStringSets(SERVER_MAP_SIZE, retrievalSize, REPEAT_TIME);
        // 随机构建服务端关键词和标签映射
        Map<String, ByteBuffer> keywordLabelMap = PirUtils.generateKeywordLabelMap(randomSets.get(0), labelByteLength);
        // 创建参与方实例
        KwPirServer<String> server = KwPirFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        KwPirClient<String> client = KwPirFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        KwPirServerThread<String> serverThread = new KwPirServerThread<>(
            server, kwPirParams, keywordLabelMap, labelByteLength, REPEAT_TIME
        );
        KwPirClientThread<String> clientThread = new KwPirClientThread<>(
            client, kwPirParams, Lists.newArrayList(randomSets.subList(1, REPEAT_TIME + 1)), labelByteLength
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
        for (int index = 0; index < REPEAT_TIME; index++) {
            Set<String> intersectionSet = new HashSet<>(randomSets.get(index + 1));
            intersectionSet.retainAll(randomSets.get(0));
            Map<String, ByteBuffer> pirResult = clientThread.getRetrievalResult(index);
            Assert.assertEquals(intersectionSet.size(), pirResult.size());
            pirResult.forEach((key, value) -> Assert.assertEquals(value, keywordLabelMap.get(key)));
        }
    }
}



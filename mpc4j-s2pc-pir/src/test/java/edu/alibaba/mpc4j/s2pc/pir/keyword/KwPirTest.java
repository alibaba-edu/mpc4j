package edu.alibaba.mpc4j.s2pc.pir.keyword;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirConfig;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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
@RunWith(Parameterized.class)
public class KwPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(KwPirTest.class);
    /**
     * 重复检索次数
     */
    private static final int REPEAT_TIME = 1;
    /**
     * 默认标签字节长度
     */
    private static final int DEFAULT_LABEL_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * 服务端元素数量
     */
    private static final int SERVER_MAP_SIZE = 1 << 20;
    /**
     * 较小客户端元素数量
     */
    private static final int SMALL_CLIENT_SET_SIZE = 1;
    /**
     * 默认客户端元素数量
     */
    private static final int DEFAULT_CLIENT_SET_SIZE = 1 << 8;
    /**
     * 较大客户端元素数量
     */
    private static final int LARGE_CLIENT_SET_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CMG21
        configurations.add(new Object[]{
            KwPirFactory.KwPirType.CMG21.name(), new Cmg21KwPirConfig.Builder().build()
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
     * 关键词索引PIR配置项
     */
    private final KwPirConfig config;

    public KwPirTest(String name, KwPirConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testSmallRetrievalSizeParallel() {
        testPir(SMALL_CLIENT_SET_SIZE, config, true);
    }

    @Test
    public void testDefaultRetrievalSizeParallel() {
        testPir(DEFAULT_CLIENT_SET_SIZE, config, true);
    }

    @Test
    public void testLargeRetrievalSizeParallel() {
        testPir(LARGE_CLIENT_SET_SIZE, config, true);
    }

    public void testPir(int retrievalSize, KwPirConfig config, boolean parallel) {
        ArrayList<Set<String>> randomSets = PirUtils.generateStringSets(SERVER_MAP_SIZE, retrievalSize, REPEAT_TIME);
        // 随机构建服务端关键词和标签映射
        Map<String, ByteBuffer> keywordLabelMap = PirUtils.generateKeywordLabelMap(randomSets.get(0), DEFAULT_LABEL_BYTE_LENGTH);
        // 创建参与方实例
        KwPirServer<String> server = KwPirFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        KwPirClient<String> client = KwPirFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        KwPirServerThread<String> serverThread = new KwPirServerThread<>(
            server, keywordLabelMap, retrievalSize, DEFAULT_LABEL_BYTE_LENGTH, REPEAT_TIME
        );
        KwPirClientThread<String> clientThread = new KwPirClientThread<>(
            client, Lists.newArrayList(randomSets.subList(1, REPEAT_TIME + 1)), retrievalSize, DEFAULT_LABEL_BYTE_LENGTH
        );
        try {
            // 开始执行协议
            serverThread.start();
            clientThread.start();
            // 等待线程停止
            serverThread.join();
            clientThread.join();

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
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }
}



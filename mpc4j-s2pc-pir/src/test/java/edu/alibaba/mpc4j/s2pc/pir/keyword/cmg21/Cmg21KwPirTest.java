package edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.*;
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
 * CMG21关键词PIR协议测试类。
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
@RunWith(Parameterized.class)
public class Cmg21KwPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Cmg21KwPirTest.class);
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
    private static final int DEFAULT_LABEL_BYTE_LENGTH = 8;
    /**
     * 长标签字节长度
     */
    private static final int LONG_LABEL_BYTE_LENGTH = 128;
    /**
     * 服务端元素数量
     */
    private static final int SERVER_MAP_SIZE = 1 << 20;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CMG21
        configurations.add(new Object[]{
            KwPirFactory.KwPirType.CMG21.name() + " max client set size 1", new Cmg21KwPirConfig.Builder().build(),
            Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_1
        });
        configurations.add(new Object[]{
            KwPirFactory.KwPirType.CMG21.name() + " max client set size 4096", new Cmg21KwPirConfig.Builder().build(),
            Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_4096
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
     * CMG21关键词PIR协议配置项
     */
    private final Cmg21KwPirConfig config;
    /**
     * CMG21关键词PIR协议配置项
     */
    private final Cmg21KwPirParams params;

    public Cmg21KwPirTest(String name, Cmg21KwPirConfig config, Cmg21KwPirParams params) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.config = config;
        this.params = params;
    }

    @Test
    public void testShortLabelParallel() {
        testPir(params, SHORT_LABEL_BYTE_LENGTH, config, true);
    }

    @Test
    public void testDefaultLabelParallel() {
        testPir(params, DEFAULT_LABEL_BYTE_LENGTH, config, true);
    }

    @Test
    public void testLongLabelParallel() {
        testPir(params, LONG_LABEL_BYTE_LENGTH, config, true);
    }

    public void testPir(Cmg21KwPirParams kwPirParams, int labelByteLength, Cmg21KwPirConfig config, boolean parallel) {
        int retrievalSize = kwPirParams.maxRetrievalSize();
        ArrayList<Set<String>> randomSets = PirUtils.generateStringSets(SERVER_MAP_SIZE, retrievalSize, REPEAT_TIME);
        // 随机构建服务端关键词和标签映射
        Map<String, ByteBuffer> keywordLabelMap = PirUtils.generateKeywordLabelMap(randomSets.get(0), labelByteLength);
        // 创建参与方实例
        Cmg21KwPirServer<String> server = new Cmg21KwPirServer<>(serverRpc, clientRpc.ownParty(), config);
        Cmg21KwPirClient<String> client = new Cmg21KwPirClient<>(clientRpc, serverRpc.ownParty(), config);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        Cmg21KwPirServerThread<String> serverThread = new Cmg21KwPirServerThread<>(
            server, kwPirParams, keywordLabelMap, labelByteLength, REPEAT_TIME
        );
        Cmg21KwPirClientThread<String> clientThread = new Cmg21KwPirClientThread<>(
            client, kwPirParams, Lists.newArrayList(randomSets.subList(1, REPEAT_TIME + 1)), labelByteLength
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



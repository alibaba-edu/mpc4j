package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.upsi.*;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * UPSI协议测试。
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
@RunWith(Parameterized.class)
public class Cmg21UpsiTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Cmg21UpsiTest.class);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CMG21
        configurations.add(new Object[]{
            UpsiFactory.UpsiType.CMG21.name(), new Cmg21UpsiConfig.Builder().build()
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
     * the unbalanced PSI config
     */
    private final UpsiConfig config;

    public Cmg21UpsiTest(String name, UpsiConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Before
    public void connect() {
        serverRpc.connect();
        clientRpc.connect();
    }

    @After
    public void disconnect() {
        serverRpc.disconnect();
        clientRpc.disconnect();
    }

    @Test
    public void test2K1() {
        testUpsi(Cmg21UpsiParams.SERVER_2K_CLIENT_MAX_1, false);
    }

    @Test
    public void test100K1() {
        testUpsi(Cmg21UpsiParams.SERVER_100K_CLIENT_MAX_1, false);
    }

    @Test
    public void test1M1024Cmp() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_1K_CMP, false);
    }

    @Test
    public void test1M1024CmpParallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_1K_CMP, true);
    }

    @Test
    public void test1M1024Com() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_1K_COM, false);
    }

    @Test
    public void test1M1024ComParallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_1K_COM, true);
    }

    @Test
    public void test1M11041Parallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_11041, true);
    }

    @Test
    public void test1M2048CmpParallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_2K_CMP, true);
    }

    @Test
    public void test1M2048ComParallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_2K_COM, true);
    }

    @Test
    public void test1M256Parallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_256, true);
    }

    @Test
    public void test1M4096CmpParallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_4K_CMP, true);
    }

    @Test
    public void test1M4096ComParallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_4K_COM, true);
    }

    @Test
    public void test1M512CmpParallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_512_CMP, true);
    }

    @Test
    public void test1M512ComParallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_512_COM, true);
    }

    @Test
    public void test1M5535Parallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_5535, true);
    }

    public void testUpsi(UpsiParams upsiParams, boolean parallel) {
        int serverSize = upsiParams.expectServerSize();
        int clientSize = upsiParams.maxClientElementSize();
        List<Set<String>> sets = PsoUtils.generateStringSets("ID", serverSize, clientSize);
        Set<String> serverElementSet = sets.get(0);
        Set<String> clientElementSet = sets.get(1);
        // 创建参与方实例
        UpsiServer<String> server = UpsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        UpsiClient<String> client = UpsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        int randomTaskId = Math.abs(new SecureRandom().nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        Cmg21UpsiServerThread<String> serverThread = new Cmg21UpsiServerThread<>(
            server, upsiParams, serverElementSet, clientElementSet.size()
        );
        Cmg21UpsiClientThread<String> clientThread = new Cmg21UpsiClientThread<>(client, upsiParams, clientElementSet);
        try {
            // 开始执行协议
            serverThread.start();
            clientThread.start();
            // 等待线程停止
            serverThread.join();
            clientThread.join();
            // 验证结果
            Set<String> psiResult = clientThread.getIntersectionSet();
            LOGGER.info("Server: The Communication costs {}MB", serverRpc.getSendByteLength() * 1.0 / (1024 * 1024));
            LOGGER.info("Client: The Communication costs {}MB", clientRpc.getSendByteLength() * 1.0 / (1024 * 1024));
            sets.get(0).retainAll(sets.get(1));
            Assert.assertTrue(sets.get(0).containsAll(psiResult));
            Assert.assertTrue(psiResult.containsAll(sets.get(0)));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }
}

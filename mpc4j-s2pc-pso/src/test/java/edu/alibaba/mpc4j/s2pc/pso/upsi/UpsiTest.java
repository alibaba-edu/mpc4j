package edu.alibaba.mpc4j.s2pc.pso.upsi;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21.Cmg21UpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.upsi.cmg21.Cmg21UpsiParams;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

/**
 * UPSI协议测试。
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
public class UpsiTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpsiTest.class);
    /**
     * 服务端
     */
    private final Rpc serverRpc;
    /**
     * 客户端
     */
    private final Rpc clientRpc;

    public UpsiTest() {
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
    }

    @Test
    public void test2K1() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        testUpsi(config, Cmg21UpsiParams.SERVER_2K_CLIENT_MAX_1, false);
    }

    @Test
    public void test100K1() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        testUpsi(config, Cmg21UpsiParams.SERVER_100K_CLIENT_MAX_1, false);
    }

    @Test
    public void test1M1024Cmp() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        testUpsi(config, Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_1K_CMP, false);
    }

    @Test
    public void test1M1024CmpParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        testUpsi(config, Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_1K_CMP, true);
    }

    @Test
    public void test1M1024Com() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        testUpsi(config, Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_1K_COM, false);
    }

    @Test
    public void test1M1024ComParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        testUpsi(config, Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_1K_COM, true);
    }

    @Test
    public void test1M11041Parallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        testUpsi(config, Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_11041, true);
    }

    @Test
    public void test1M2048CmpParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        testUpsi(config, Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_2K_CMP, true);
    }

    @Test
    public void test1M2048ComParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        testUpsi(config, Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_2K_COM, true);
    }

    @Test
    public void test1M256Parallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        testUpsi(config, Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_256, true);
    }

    @Test
    public void test1M4096CmpParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        testUpsi(config, Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_4K_CMP, true);
    }

    @Test
    public void test1M4096ComParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        testUpsi(config, Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_4K_COM, true);
    }

    @Test
    public void test1M512CmpParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        testUpsi(config, Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_512_CMP, true);
    }

    @Test
    public void test1M512ComParallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        testUpsi(config, Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_512_COM, true);
    }

    @Test
    public void test1M5535Parallel() {
        Cmg21UpsiConfig config = new Cmg21UpsiConfig.Builder().build();
        testUpsi(config, Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_5535, true);
    }

    public void testUpsi(Cmg21UpsiConfig config, UpsiParams upsiParams, boolean parallel) {
        int serverSize = upsiParams.expectServerSize();
        int clientSize = upsiParams.maxClientSize();
        List<Set<String>> sets = PsoUtils.generateStringSets("ID", serverSize, clientSize);
        Set<String> serverElementSet = sets.get(0);
        Set<String> clientElementSet = sets.get(1);
        // 创建参与方实例
        UpsiServer<String> server = UpsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        UpsiClient<String> client = UpsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        long randomTaskId = Math.abs(new SecureRandom().nextLong());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        // 设置并发
        server.setParallel(parallel);
        client.setParallel(parallel);
        UpsiServerThread<String> serverThread = new UpsiServerThread<>(
            server, upsiParams, serverElementSet, clientElementSet.size()
        );
        UpsiClientThread<String> clientThread = new UpsiClientThread<>(client, upsiParams, clientElementSet);
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
        // 验证结果
        Set<String> psiResult = clientThread.getIntersectionSet();
        LOGGER.info("Server: The Communication costs {}MB", serverRpc.getSendByteLength() * 1.0 / (1024 * 1024));
        LOGGER.info("Client: The Communication costs {}MB", clientRpc.getSendByteLength() * 1.0 / (1024 * 1024));
        sets.get(0).retainAll(sets.get(1));
        Assert.assertTrue(sets.get(0).containsAll(psiResult));
        Assert.assertTrue(psiResult.containsAll(sets.get(0)));
    }
}

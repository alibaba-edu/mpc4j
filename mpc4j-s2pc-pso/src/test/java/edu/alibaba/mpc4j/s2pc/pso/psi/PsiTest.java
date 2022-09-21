package edu.alibaba.mpc4j.s2pc.pso.psi;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.hfh99.Hfh99ByteEccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.hfh99.Hfh99EccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.kkrt16.Kkrt16PsiConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * PSI协议测试。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
@RunWith(Parameterized.class)
public class PsiTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsiTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量
     */
    private static final int DEFAULT_SIZE = 99;
    /**
     * 元素字节长度
     */
    private static final int ELEMENT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * 较大数量
     */
    private static final int LARGE_SIZE = 1 << 14;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // KKRT16 (no-stash)
        configurations.add(new Object[] {
            PsiFactory.PsiType.KKRT16.name() + " (no-stash)",
            new Kkrt16PsiConfig.Builder().setCuckooHashBinType(CuckooHashBinType.NO_STASH_NAIVE).build(),
        });
        // KKRT16 (4 hash)
        configurations.add(new Object[] {
            PsiFactory.PsiType.KKRT16.name() + " (4 hash)",
            new Kkrt16PsiConfig.Builder().setCuckooHashBinType(CuckooHashBinType.NAIVE_4_HASH).build(),
        });
        // KKRT16
        configurations.add(new Object[] {
            PsiFactory.PsiType.KKRT16.name(), new Kkrt16PsiConfig.Builder().build(),
        });
        // HFH99_BYTE_ECC
        configurations.add(new Object[] {
            PsiFactory.PsiType.HFH99_BYTE_ECC.name(), new Hfh99ByteEccPsiConfig.Builder().build(),
        });
        // HFH99_ECC (compress)
        configurations.add(new Object[] {
            PsiFactory.PsiType.HFH99_ECC.name() + " (compress)",
            new Hfh99EccPsiConfig.Builder().setCompressEncode(true).build(),
        });
        // HFH99_ECC (uncompress)
        configurations.add(new Object[] {
            PsiFactory.PsiType.HFH99_ECC.name() + " (uncompress)",
            new Hfh99EccPsiConfig.Builder().setCompressEncode(false).build(),
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
     * 协议类型
     */
    private final PsiConfig config;

    public PsiTest(String name, PsiConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        PsiServer<ByteBuffer> server = PsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsiClient<ByteBuffer> client = PsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), server.getPtoType());
        Assert.assertEquals(config.getPtoType(), client.getPtoType());
    }

    @Test
    public void test1() {
        PsiServer<ByteBuffer> server = PsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsiClient<ByteBuffer> client = PsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPto(server, client, 1, 1);
    }

    @Test
    public void test2() {
        PsiServer<ByteBuffer> server = PsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsiClient<ByteBuffer> client = PsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPto(server, client, 2, 2);
    }

    @Test
    public void test10() {
        PsiServer<ByteBuffer> server = PsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsiClient<ByteBuffer> client = PsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPto(server, client, 10, 10);
    }

    @Test
    public void testLargeServerSize() {
        PsiServer<ByteBuffer> server = PsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsiClient<ByteBuffer> client = PsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPto(server, client, DEFAULT_SIZE, 10);
    }

    @Test
    public void testLargeClientSize() {
        PsiServer<ByteBuffer> server = PsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsiClient<ByteBuffer> client = PsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPto(server, client, 10, DEFAULT_SIZE);
    }

    @Test
    public void testDefault() {
        PsiServer<ByteBuffer> server = PsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsiClient<ByteBuffer> client = PsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPto(server, client, DEFAULT_SIZE, DEFAULT_SIZE);
    }

    @Test
    public void testParallelDefault() {
        PsiServer<ByteBuffer> server = PsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsiClient<ByteBuffer> client = PsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        server.setParallel(true);
        client.setParallel(true);
        testPto(server, client, DEFAULT_SIZE, DEFAULT_SIZE);
    }

    @Test
    public void testLarge() {
        PsiServer<ByteBuffer> server = PsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsiClient<ByteBuffer> client = PsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPto(server, client, LARGE_SIZE, LARGE_SIZE);
    }

    @Test
    public void testParallelLarge() {
        PsiServer<ByteBuffer> server = PsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsiClient<ByteBuffer> client = PsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        server.setParallel(true);
        client.setParallel(true);
        testPto(server, client, LARGE_SIZE, LARGE_SIZE);
    }

    private void testPto(PsiServer<ByteBuffer> server, PsiClient<ByteBuffer> client, int serverSize, int clientSize) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，server_size = {}，client_size = {}-----",
                server.getPtoDesc().getPtoName(), serverSize, clientSize
            );
            // 生成集合
            ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSize, clientSize, ELEMENT_BYTE_LENGTH);
            Set<ByteBuffer> serverSet = sets.get(0);
            Set<ByteBuffer> clientSet = sets.get(1);
            // 构建线程
            PsiServerThread serverThread = new PsiServerThread(server, serverSet, clientSet.size());
            PsiClientThread clientThread = new PsiClientThread(client, clientSet, serverSet.size());
            StopWatch stopWatch = new StopWatch();
            // 开始执行协议
            stopWatch.start();
            serverThread.start();
            clientThread.start();
            // 等待线程停止
            serverThread.join();
            clientThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // 验证结果
            assertOutput(serverSet, clientSet, clientThread.getIntersectionSet());
            LOGGER.info("Server data_packet_num = {}, payload_bytes = {}B, send_bytes = {}B, time = {}ms",
                serverRpc.getSendDataPacketNum(), serverRpc.getPayloadByteLength(), serverRpc.getSendByteLength(),
                time
            );
            LOGGER.info("Client data_packet_num = {}, payload_bytes = {}B, send_bytes = {}B, time = {}ms",
                clientRpc.getSendDataPacketNum(), clientRpc.getPayloadByteLength(), clientRpc.getSendByteLength(),
                time
            );
            serverRpc.reset();
            clientRpc.reset();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(Set<ByteBuffer> serverSet, Set<ByteBuffer> clientSet, Set<ByteBuffer> outputIntersectionSet) {
        Set<ByteBuffer> expectIntersectionSet = new HashSet<>(serverSet);
        expectIntersectionSet.retainAll(clientSet);
        Assert.assertTrue(outputIntersectionSet.containsAll(expectIntersectionSet));
        Assert.assertTrue(expectIntersectionSet.containsAll(outputIntersectionSet));
    }
}

package edu.alibaba.mpc4j.s2pc.pso.pmid;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.pmid.zcl22.Zcl22MpPmidConfig;
import edu.alibaba.mpc4j.s2pc.pso.pmid.zcl22.Zcl22SloppyPmidConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfcPsuConfig;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 双方集合PMID测试。
 *
 * @author Weiran Liu
 * @date 2022/08/26
 */
@RunWith(Parameterized.class)
public class BothSetPmidTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerSetPmidTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量
     */
    private static final int DEFAULT_SET_SIZE = 1 << 8;
    /**
     * 较大数量
     */
    private static final int LARGE_SET_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // ZCL22_SLOPPY (MEGA_BIN)
        configurationParams.add(new Object[] {
            PmidFactory.PmidType.ZCL22_SLOPPY.name() + " (MEGA_BIN)" ,
            new Zcl22SloppyPmidConfig.Builder().setSigmaOkvsType(OkvsFactory.OkvsType.MEGA_BIN).build(),
        });
        // ZCL22_SLOPPY (H3_SINGLETON_GCT)
        configurationParams.add(new Object[] {
            PmidFactory.PmidType.ZCL22_SLOPPY.name() + " (H3_SINGLETON_GCT)",
            new Zcl22SloppyPmidConfig.Builder().setSigmaOkvsType(OkvsFactory.OkvsType.H3_SINGLETON_GCT).build(),
        });
        // ZCL22_SLOPPY (JSZ22_SFC_PSU)
        configurationParams.add(new Object[] {
            PmidFactory.PmidType.ZCL22_SLOPPY.name() + " (JSZ22_SFC_PSU)",
            new Zcl22SloppyPmidConfig.Builder().setPsuConfig(new Jsz22SfcPsuConfig.Builder().build()).build(),
        });

        // ZCL22_MP (MEGA_BIN)
        configurationParams.add(new Object[] {
            PmidFactory.PmidType.ZCL22_MP.name() + " (MEGA_BIN)" ,
            new Zcl22MpPmidConfig.Builder().setSigmaOkvsType(OkvsFactory.OkvsType.MEGA_BIN).build(),
        });
        // ZCL22_MP (H3_SINGLETON_GCT)
        configurationParams.add(new Object[] {
            PmidFactory.PmidType.ZCL22_MP.name() + " (H3_SINGLETON_GCT)",
            new Zcl22MpPmidConfig.Builder().setSigmaOkvsType(OkvsFactory.OkvsType.H3_SINGLETON_GCT).build(),
        });
        // ZCL22_MP (JSZ22_SFC_PSU)
        configurationParams.add(new Object[] {
            PmidFactory.PmidType.ZCL22_MP.name() + " (JSZ22_SFC_PSU)",
            new Zcl22MpPmidConfig.Builder().setPsuConfig(new Jsz22SfcPsuConfig.Builder().build()).build(),
        });

        return configurationParams;
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
    private final PmidConfig config;

    public BothSetPmidTest(String name, PmidConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        PmidServer<String> server = PmidFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PmidClient<String> client = PmidFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), server.getPtoType());
        Assert.assertEquals(config.getPtoType(), client.getPtoType());
    }

    @Test
    public void test2() {
        PmidServer<String> server = PmidFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PmidClient<String> client = PmidFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPmid(server, client, 2, 2);
    }

    @Test
    public void test10() {
        PmidServer<String> server = PmidFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PmidClient<String> client = PmidFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPmid(server, client, 10, 10);
    }

    @Test
    public void testLargeServerSize() {
        PmidServer<String> server = PmidFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PmidClient<String> client = PmidFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPmid(server, client, LARGE_SET_SIZE, DEFAULT_SET_SIZE);
    }

    @Test
    public void testLargeClientSize() {
        PmidServer<String> server = PmidFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PmidClient<String> client = PmidFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPmid(server, client, DEFAULT_SET_SIZE, LARGE_SET_SIZE);
    }

    @Test
    public void testDefault() {
        PmidServer<String> server = PmidFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PmidClient<String> client = PmidFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPmid(server, client, DEFAULT_SET_SIZE, DEFAULT_SET_SIZE);
    }

    @Test
    public void testParallelDefault() {
        PmidServer<String> server = PmidFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PmidClient<String> client = PmidFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        server.setParallel(true);
        client.setParallel(true);
        testPmid(server, client, DEFAULT_SET_SIZE, DEFAULT_SET_SIZE);
    }

    @Test
    public void testLarge() {
        PmidServer<String> server = PmidFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PmidClient<String> client = PmidFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPmid(server, client, LARGE_SET_SIZE, LARGE_SET_SIZE);
    }

    @Test
    public void testParallelLarge() {
        PmidServer<String> server = PmidFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PmidClient<String> client = PmidFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        server.setParallel(true);
        client.setParallel(true);
        testPmid(server, client, LARGE_SET_SIZE, LARGE_SET_SIZE);
    }

    private void testPmid(PmidServer<String> server, PmidClient<String> client, int serverSetSize, int clientSetSize) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，server set size = {}, client set size = {}-----",
                server.getPtoDesc().getPtoName(), serverSetSize, clientSetSize
            );
            // 生成集合和映射
            ArrayList<Set<String>> sets = PsoUtils.generateStringSets("ID", serverSetSize, clientSetSize);
            Set<String> serverSet = sets.get(0);
            Set<String> clientSet = sets.get(1);
            // 构建线程
            BothSetPmidServerThread serverThread = new BothSetPmidServerThread(server, serverSet, clientSet.size());
            BothSetPmidClientThread clientThread = new BothSetPmidClientThread(client, clientSet, serverSet.size());
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
            assertOutput(serverSet, clientSet, serverThread.getServerOutput(), clientThread.getClientOutput());
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

    private void assertOutput(Set<String> serverSet, Set<String> clientSet,
                              PmidPartyOutput<String> serverOutput, PmidPartyOutput<String> clientOutput) {
        Assert.assertEquals(serverOutput.getPmidByteLength(), clientOutput.getPmidByteLength());
        // 计算交集
        Set<String> intersection = new HashSet<>();
        serverSet.forEach(serverElement -> {
            if (clientSet.contains(serverElement)) {
                intersection.add(serverElement);
            }
        });
        // 计算并集
        Set<String> union = new HashSet<>(serverSet);
        union.addAll(clientSet);
        // 得到PMID集合
        Set<ByteBuffer> serverPmidSet = serverOutput.getPmidSet();
        Set<ByteBuffer> clientPmidSet = clientOutput.getPmidSet();
        // 查看PMID数量
        int pmidSetSize = union.size();
        Assert.assertEquals(pmidSetSize, serverPmidSet.size());
        Assert.assertEquals(pmidSetSize, clientPmidSet.size());
        // 验证PMID相等
        Assert.assertTrue(serverPmidSet.containsAll(clientPmidSet));
        Assert.assertTrue(clientPmidSet.containsAll(serverPmidSet));
        // 计算PID交集
        Set<String> intersectionSet = new HashSet<>();
        serverPmidSet.forEach(pmid -> {
            String serverId = serverOutput.getId(pmid);
            String clientId = clientOutput.getId(pmid);
            if (serverId != null && clientId != null) {
                Assert.assertEquals(serverId, clientId);
                intersectionSet.add(serverId);
            }
        });
        Assert.assertTrue(intersectionSet.containsAll(intersection));
        Assert.assertTrue(intersection.containsAll(intersectionSet));
    }
}

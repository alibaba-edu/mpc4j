package edu.alibaba.mpc4j.s2pc.pso.pid;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.pid.bkms20.Bkms20ByteEccPidConfig;
import edu.alibaba.mpc4j.s2pc.pso.pid.bkms20.Bkms20EccPidConfig;
import edu.alibaba.mpc4j.s2pc.pso.pid.gmr21.Gmr21MpPidConfig;
import edu.alibaba.mpc4j.s2pc.pso.pid.gmr21.Gmr21SloppyPidConfig;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * PID协议测试。注意，PID参与方的输入集合大小至少大于1。
 *
 * @author Weiran Liu
 * @date 2019/07/12
 */
@RunWith(Parameterized.class)
public class PidTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PidTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量
     */
    private static final int DEFAULT_SIZE = 1 << 10;
    /**
     * 较大数量
     */
    private static final int LARGE_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // GMR21_MP
        configurationParams.add(new Object[] {
            PidFactory.PidType.GMR21_MP.name(),
            new Gmr21MpPidConfig.Builder().build(),
        });
        // GMR21_MP (JSZ22_SFC_PSU)
        configurationParams.add(new Object[] {
            PidFactory.PidType.GMR21_MP.name() + " (JSZ22_SFC_PSU)",
            new Gmr21MpPidConfig.Builder().setPsuConfig(new Jsz22SfcPsuConfig.Builder().build()).build(),
        });

        // GMR21_SLOPPY (MEGA_BIN)
        configurationParams.add(new Object[] {
            PidFactory.PidType.GMR21_SLOPPY.name() + " (MEGA_BIN)",
            new Gmr21SloppyPidConfig.Builder().setSloppyOkvsType(OkvsType.MEGA_BIN).build(),
        });
        // GMR21_SLOPPY (H3_SINGLETON_GCT)
        configurationParams.add(new Object[] {
            PidFactory.PidType.GMR21_SLOPPY.name() + " (H3_SINGLETON_GCT)",
            new Gmr21SloppyPidConfig.Builder().setSloppyOkvsType(OkvsType.H3_SINGLETON_GCT).build(),
        });
        // GMR21_SLOPPY (JSZ22_SFC_PSU)
        configurationParams.add(new Object[] {
            PidFactory.PidType.GMR21_SLOPPY.name() + " (JSZ22_SFC_PSU)",
            new Gmr21SloppyPidConfig.Builder().setPsuConfig(new Jsz22SfcPsuConfig.Builder().build()).build(),
        });

        // BKMS20_BYTE_ECC
        configurationParams.add(new Object[] {
            PidFactory.PidType.BKMS20_BYTE_ECC.name(), new Bkms20ByteEccPidConfig.Builder().build(),
        });

        // BKMS20_ECC (compress)
        configurationParams.add(new Object[] {
            PidFactory.PidType.BKMS20_ECC.name() + " (compress)",
            new Bkms20EccPidConfig.Builder().setCompressEncode(true).build(),
        });
        // BKMS20_ECC (uncompress)
        configurationParams.add(new Object[] {
            PidFactory.PidType.BKMS20_ECC.name() + " (uncompress)",
            new Bkms20EccPidConfig.Builder().setCompressEncode(false).build(),
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
    private final PidConfig config;

    public PidTest(String name, PidConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        PidParty<String> server = PidFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PidParty<String> client = PidFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), server.getPtoType());
        Assert.assertEquals(config.getPtoType(), client.getPtoType());
    }

    @Test
    public void test2() {
        PidParty<String> server = PidFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PidParty<String> client = PidFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPid(server, client, 2);
    }

    @Test
    public void test10() {
        PidParty<String> server = PidFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PidParty<String> client = PidFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPid(server, client, 10);
    }

    @Test
    public void testDefault() {
        PidParty<String> server = PidFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PidParty<String> client = PidFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPid(server, client, DEFAULT_SIZE);
    }

    @Test
    public void testParallelDefault() {
        PidParty<String> server = PidFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PidParty<String> client = PidFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        server.setParallel(true);
        client.setParallel(true);
        testPid(server, client, DEFAULT_SIZE);
    }

    @Test
    public void testLarge() {
        PidParty<String> server = PidFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PidParty<String> client = PidFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPid(server, client, LARGE_SIZE);
    }

    @Test
    public void testParallelLarge() {
        PidParty<String> server = PidFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PidParty<String> client = PidFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        server.setParallel(true);
        client.setParallel(true);
        testPid(server, client, LARGE_SIZE);
    }

    private void testPid(PidParty<String> server, PidParty<String> client, int size) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，size = {}-----", server.getPtoDesc().getPtoName(), size);
            // 生成集合
            ArrayList<Set<String>> sets = PsoUtils.generateEqualStringSets("ID", 2, size);
            Set<String> serverSet = sets.get(0);
            Set<String> clientSet = sets.get(1);
            // 构建线程
            PidPartyThread serverThread = new PidPartyThread(server, serverSet, clientSet.size());
            PidPartyThread clientThread = new PidPartyThread(client, clientSet, serverSet.size());
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
            assertOutput(serverSet, clientSet, serverThread.getPidOutput(), clientThread.getPidOutput());
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
        PidPartyOutput<String> serverOutput, PidPartyOutput<String> clientOutput) {
        Assert.assertEquals(serverOutput.getPidByteLength(), clientOutput.getPidByteLength());
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
        // 得到PID集合
        Set<ByteBuffer> serverPidSet = serverOutput.getPidSet();
        Set<ByteBuffer> clientPidSet = clientOutput.getPidSet();
        // 查看PID数量
        Assert.assertEquals(union.size(), serverPidSet.size());
        Assert.assertEquals(union.size(), clientPidSet.size());
        // 验证PID映射数量
        Assert.assertEquals(serverSet.size(), serverOutput.getIdSet().size());
        Assert.assertEquals(clientSet.size(), clientOutput.getIdSet().size());
        // 验证PID相等
        Assert.assertTrue(serverPidSet.containsAll(clientPidSet));
        Assert.assertTrue(clientPidSet.containsAll(serverPidSet));
        // 计算PID交集
        Set<String> intersectionSet = new HashSet<>();
        serverPidSet.forEach(pid -> {
            String serverId = serverOutput.getId(pid);
            String clientId = clientOutput.getId(pid);
            if (serverId != null && clientId != null) {
                Assert.assertEquals(serverId, clientId);
                intersectionSet.add(serverId);
            }
        });
        Assert.assertTrue(intersectionSet.containsAll(intersection));
        Assert.assertTrue(intersection.containsAll(intersectionSet));
    }
}
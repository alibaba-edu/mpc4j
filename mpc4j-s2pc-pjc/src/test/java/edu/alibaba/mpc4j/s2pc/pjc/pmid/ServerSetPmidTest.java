package edu.alibaba.mpc4j.s2pc.pjc.pmid;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22.Zcl22MpPmidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22.Zcl22SloppyPmidConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfcPsuConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 服务端集合PMID协议测试。
 *
 * @author Weiran Liu
 * @date 2022/5/10
 */
@RunWith(Parameterized.class)
public class ServerSetPmidTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerSetPmidTest.class);
    /**
     * 默认数量
     */
    private static final int DEFAULT_SET_SIZE = 1 << 8;
    /**
     * 较大数量
     */
    private static final int LARGE_SET_SIZE = 1 << 12;
    /**
     * 客户端较小重复元素上界
     */
    private static final int SMALL_MAX_CLIENT_U = 2;
    /**
     * 客户端默认重复元素上界
     */
    private static final int DEFAULT_MAX_CLIENT_U = 3;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // ZCL22_SLOPPY (MEGA_BIN)
        configurations.add(new Object[] {
            PmidFactory.PmidType.ZCL22_SLOPPY.name() + " (" + Gf2eDokvsType.MEGA_BIN + ")",
            new Zcl22SloppyPmidConfig.Builder().setSigmaOkvsType(Gf2eDokvsType.MEGA_BIN).build(),
        });
        // ZCL22_SLOPPY (H3_NAIVE_CLUSTER_BLAZE_GCT)
        configurations.add(new Object[] {
            PmidFactory.PmidType.ZCL22_SLOPPY.name() + " (" + Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT + ")",
            new Zcl22SloppyPmidConfig.Builder().setSigmaOkvsType(Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT).build(),
        });
        // ZCL22_SLOPPY (JSZ22_SFC_PSU)
        configurations.add(new Object[] {
            PmidFactory.PmidType.ZCL22_SLOPPY.name() + " (" + PsuType.JSZ22_SFC + ")",
            new Zcl22SloppyPmidConfig.Builder().setPsuConfig(new Jsz22SfcPsuConfig.Builder(false).build()).build(),
        });
        // ZCL22_MP (MEGA_BIN)
        configurations.add(new Object[] {
            PmidFactory.PmidType.ZCL22_MP.name() + " (" + Gf2eDokvsType.MEGA_BIN + ")" ,
            new Zcl22MpPmidConfig.Builder().setSigmaOkvsType(Gf2eDokvsType.MEGA_BIN).build(),
        });
        // ZCL22_MP (H3_NAIVE_CLUSTER_BLAZE_GCT)
        configurations.add(new Object[] {
            PmidFactory.PmidType.ZCL22_MP.name() + " (" + Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT + ")",
            new Zcl22MpPmidConfig.Builder().setSigmaOkvsType(Gf2eDokvsType.H3_NAIVE_CLUSTER_BLAZE_GCT).build(),
        });
        // ZCL22_MP (JSZ22_SFC_PSU)
        configurations.add(new Object[] {
            PmidFactory.PmidType.ZCL22_MP.name() + " (" + PsuType.JSZ22_SFC + ")",
            new Zcl22MpPmidConfig.Builder().setPsuConfig(new Jsz22SfcPsuConfig.Builder(false).build()).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final PmidConfig config;

    public ServerSetPmidTest(String name, PmidConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test2() {
        testPmid(2, 2, DEFAULT_MAX_CLIENT_U, false);
    }

    @Test
    public void test10() {
        testPmid(10, 10, DEFAULT_MAX_CLIENT_U, false);
    }

    @Test
    public void testSmallU() {
        testPmid(DEFAULT_SET_SIZE, DEFAULT_SET_SIZE, SMALL_MAX_CLIENT_U, false);
    }

    @Test
    public void testLargeServerSize() {
        testPmid(LARGE_SET_SIZE, DEFAULT_SET_SIZE, DEFAULT_MAX_CLIENT_U, false);
    }

    @Test
    public void testLargeClientSize() {
        testPmid(DEFAULT_SET_SIZE, LARGE_SET_SIZE, DEFAULT_MAX_CLIENT_U, false);
    }

    @Test
    public void testDefault() {
        testPmid(DEFAULT_SET_SIZE, DEFAULT_SET_SIZE, DEFAULT_MAX_CLIENT_U, false);
    }

    @Test
    public void testParallelDefault() {
        testPmid(DEFAULT_SET_SIZE, DEFAULT_SET_SIZE, DEFAULT_MAX_CLIENT_U, true);
    }

    @Test
    public void testLarge() {
        testPmid(LARGE_SET_SIZE, LARGE_SET_SIZE, DEFAULT_MAX_CLIENT_U, false);
    }

    @Test
    public void testParallelLarge() {
        testPmid(LARGE_SET_SIZE, LARGE_SET_SIZE, DEFAULT_MAX_CLIENT_U, true);
    }

    private void testPmid(int serverSetSize, int clientSetSize, int maxClientU, boolean parallel) {
        PmidServer<String> server = PmidFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        PmidClient<String> client = PmidFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，server set size = {}, client set size = {}, max(ClientK) = {}-----",
                server.getPtoDesc().getPtoName(), serverSetSize, clientSetSize, maxClientU
            );
            // generate sets and map
            ArrayList<Set<String>> sets = PsoUtils.generateStringSets("ID", serverSetSize, clientSetSize);
            Set<String> serverSet = sets.get(0);
            Set<String> clientSet = sets.get(1);
            Map<String, Integer> clientMap = clientSet.stream().collect(Collectors.toMap(
                element -> element,
                element -> SECURE_RANDOM.nextInt(maxClientU) + 1
            ));
            int clientU = clientSet.stream().mapToInt(clientMap::get).max().orElse(0);
            ServerSetPmidServerThread serverThread = new ServerSetPmidServerThread(server, serverSet, clientSet.size(), maxClientU, clientU);
            ServerSetPmidClientThread clientThread = new ServerSetPmidClientThread(client, clientMap, maxClientU, serverSet.size());
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            serverThread.start();
            clientThread.start();
            // stop
            serverThread.join();
            clientThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            assertOutput(serverSet, clientMap, serverThread.getServerOutput(), clientThread.getClientOutput());
            printAndResetRpc(time);
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(Set<String> serverSet, Map<String, Integer> clientMap,
                              PmidPartyOutput<String> serverOutput, PmidPartyOutput<String> clientOutput) {
        Assert.assertEquals(serverOutput.getPmidByteLength(), clientOutput.getPmidByteLength());
        // 计算交集
        Set<String> intersection = new HashSet<>();
        serverSet.forEach(serverElement -> {
            if (clientMap.containsKey(serverElement)) {
                intersection.add(serverElement);
            }
        });
        // 计算并集
        Set<String> union = new HashSet<>(serverSet);
        union.addAll(clientMap.keySet());
        // 得到PMID集合
        Set<ByteBuffer> serverPmidSet = serverOutput.getPmidSet();
        Set<ByteBuffer> clientPmidSet = clientOutput.getPmidSet();
        // 查看PMID数量
        int pmidSetSize = union.stream()
            .mapToInt(element -> {
                if (intersection.contains(element)) {
                    return clientMap.get(element);
                } else {
                    return clientMap.getOrDefault(element, 1);
                }
            })
            .sum();
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

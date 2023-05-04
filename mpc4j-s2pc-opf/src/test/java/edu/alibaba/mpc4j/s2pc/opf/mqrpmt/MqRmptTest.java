package edu.alibaba.mpc4j.s2pc.opf.mqrpmt;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.opf.OpfUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct.DirectCotConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz22.Czz22ByteEccCwMqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.gmr21.Gmr21OsnConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * mqRPMT协议测试。
 *
 * @author Weiran Liu
 * @date 2022/09/10
 */
@RunWith(Parameterized.class)
public class MqRmptTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqRmptTest.class);
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * 默认数量
     */
    private static final int DEFAULT_SIZE = 1000;
    /**
     * 默认元素字节长度
     */
    private static final int ELEMENT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * 较大数量
     */
    private static final int LARGE_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // 用直接COT实现的OSN
        OsnConfig directCotOsnConfig = new Gmr21OsnConfig.Builder()
            .setCotConfig(new DirectCotConfig.Builder(SecurityModel.SEMI_HONEST).build())
            .build();
        // GMR21
        configurationParams.add(new Object[]{
            MqRpmtFactory.MqRpmtType.GMR21.name(),
            new Gmr21MqRpmtConfig.Builder().setOsnConfig(directCotOsnConfig).build(),
        });
        // CZZ22_BYTE_ECC_CW
        configurationParams.add(new Object[]{
            MqRpmtFactory.MqRpmtType.CZZ22_BYTE_ECC_CW.name(), new Czz22ByteEccCwMqRpmtConfig.Builder().build(),
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
    private final MqRpmtConfig config;

    public MqRmptTest(String name, MqRpmtConfig config) {
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
    public void test2() {
        testPto(2, 2, false);
    }

    @Test
    public void test10() {
        testPto(10, 10, false);
    }

    @Test
    public void testLargeServerSize() {
        testPto(DEFAULT_SIZE, 10, false);
    }

    @Test
    public void testLargeClientSize() {
        testPto(10, DEFAULT_SIZE, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, true);
    }

    @Test
    public void testLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, true);
    }

    private void testPto(int serverSize, int clientSize, boolean parallel) {
        MqRpmtServer server = MqRpmtFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        MqRpmtClient client = MqRpmtFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，server_size = {}，client_size = {}-----",
                server.getPtoDesc().getPtoName(), serverSize, clientSize
            );
            // 生成集合
            ArrayList<Set<ByteBuffer>> sets = OpfUtils.generateBytesSets(serverSize, clientSize, ELEMENT_BYTE_LENGTH);
            Set<ByteBuffer> serverSet = sets.get(0);
            Set<ByteBuffer> clientSet = sets.get(1);
            // 构建线程
            MqRpmtServerThread serverThread = new MqRpmtServerThread(server, serverSet, clientSet.size());
            MqRpmtClientThread clientThread = new MqRpmtClientThread(client, clientSet, serverSet.size());
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
            ByteBuffer[] serverVector = serverThread.getServerVector();
            boolean[] containVector = clientThread.getContainVector();
            assertOutput(serverVector, clientSet, containVector);
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
        server.destroy();
        client.destroy();
    }

    private void assertOutput(ByteBuffer[] serverVector, Set<ByteBuffer> clientSet, boolean[] containVector) {
        Assert.assertEquals(serverVector.length, containVector.length);
        int vectorLength = serverVector.length;
        IntStream.range(0, vectorLength).forEach(index ->
            Assert.assertEquals(clientSet.contains(serverVector[index]), containVector[index])
        );
    }
}

package edu.alibaba.mpc4j.s2pc.pso.psu;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.ecc.EccOvdmFactory.EccOvdmType;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.gf2e.Gf2eOvdmFactory.Gf2eOvdmType;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcConfig;
import edu.alibaba.mpc4j.s2pc.aby.bc.bea91.Bea91BcConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.file.FileZ2MtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct.DirectCotConfig;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.oprp.OprpConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprp.lowmc.LowMcOprpConfig;
import edu.alibaba.mpc4j.s2pc.pso.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.pso.osn.gmr21.Gmr21OsnConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.gmr21.Gmr21PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfcPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfsPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.krtw19.Krtw19OptPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.krtw19.Krtw19OriPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl22.Zcl22PkePsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl22.Zcl22SkePsuConfig;
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
 * PSU协议测试。
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
@RunWith(Parameterized.class)
public class PsuTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsuTest.class);
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
    private static final int DEFAULT_ELEMENT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * 较小元素字节长度
     */
    private static final int SMALL_ELEMENT_BYTE_LENGTH = Long.BYTES;
    /**
     * 较大元素字节长度
     */
    private static final int LARGE_ELEMENT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH * 2;
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

        // JSZ22_SFS (direct COT, NAIVE_3_HASH)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.JSZ22_SFS.name() + " (direct COT, NAIVE_3_HASH)",
            new Jsz22SfsPsuConfig.Builder().setOsnConfig(directCotOsnConfig).build(),
        });
        // JSZ22_SFS (silent OT, NAIVE_3_HASH)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.JSZ22_SFS.name() + " (silent COT, NAIVE_3_HASH)",
            new Jsz22SfsPsuConfig.Builder().build(),
        });
        // JSZ22_SFS (direct COT, NAIVE_4_HASH)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.JSZ22_SFS.name() + " (direct COT, NAIVE_4_HASH)",
            new Jsz22SfsPsuConfig.Builder()
                .setOsnConfig(directCotOsnConfig)
                .setCuckooHashBinType(CuckooHashBinType.NAIVE_4_HASH)
                .build(),
        });
        // JSZ22_SFS (direct COT, NO_STASH_PSZ18_3_HASH)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.JSZ22_SFS.name() + " (direct COT, NO_STASH_PSZ18_3_HASH)",
            new Jsz22SfsPsuConfig.Builder()
                .setOsnConfig(directCotOsnConfig)
                .setCuckooHashBinType(CuckooHashBinType.NO_STASH_PSZ18_3_HASH)
                .build(),
        });

        // JSZ22_SFC (direct COT, NAIVE_3_HASH)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.JSZ22_SFC.name() + " (direct COT, NAIVE_3_HASH)",
            new Jsz22SfcPsuConfig.Builder().setOsnConfig(directCotOsnConfig).build(),
        });
        // JSZ22_SFC (silent COT, NAIVE_3_HASH)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.JSZ22_SFC.name() + " (silent COT, NAIVE_3_HASH)",
            new Jsz22SfcPsuConfig.Builder().build(),
        });
        // JSZ22_SFC (direct COT, NAIVE_4_HASH)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.JSZ22_SFC.name() + " (direct COT, NAIVE_4_HASH)",
            new Jsz22SfcPsuConfig.Builder()
                .setOsnConfig(directCotOsnConfig)
                .setCuckooHashBinType(CuckooHashBinType.NAIVE_4_HASH)
                .build(),
        });
        // JSZ22_SFC (direct COT, NO_STASH_PSZ18_3_HASH)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.JSZ22_SFC.name() + " (direct COT, NO_STASH_PSZ18_3_HASH)",
            new Jsz22SfcPsuConfig.Builder()
                .setOsnConfig(directCotOsnConfig)
                .setCuckooHashBinType(CuckooHashBinType.NO_STASH_PSZ18_3_HASH)
                .build(),
        });

        // ZCL22_PKE (H3_SINGLETON_GCT)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.ZCL22_PKE.name() + " (H3_SINGLETON_GCT)",
            new Zcl22PkePsuConfig.Builder().setEccOvdmType(EccOvdmType.H3_SINGLETON_GCT).build(),
        });
        // ZCL22_PKE (H2_SINGLETON_GCT)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.ZCL22_PKE.name() + " (H3_SINGLETON_GCT)",
            new Zcl22PkePsuConfig.Builder().setEccOvdmType(EccOvdmType.H2_SINGLETON_GCT).build(),
        });

        // ZCL22_SKE (H3_SINGLETON_GCT)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.ZCL22_SKE.name() + " (H3_SINGLETON_GCT)",
            new Zcl22SkePsuConfig.Builder().setGf2eOvdmType(Gf2eOvdmType.H3_SINGLETON_GCT).build(),
        });
        // ZCL22_SKE (H2_SINGLETON_GCT)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.ZCL22_SKE.name() + " (H2_SINGLETON_GCT)",
            new Zcl22SkePsuConfig.Builder().setGf2eOvdmType(Gf2eOvdmType.H2_SINGLETON_GCT).build(),
        });
        // ZCL22_SKE (file Z2_MTG)
        Z2MtgConfig fileZ2MtgConfig = new FileZ2MtgConfig.Builder(SecurityModel.SEMI_HONEST).build();
        BcConfig fileBcConfig = new Bea91BcConfig.Builder().setZ2MtgConfig(fileZ2MtgConfig).build();
        OprpConfig fileOprpConfig = new LowMcOprpConfig.Builder().setBcConfig(fileBcConfig).build();
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.ZCL22_SKE.name() + " (File Z2_MTG)",
            new Zcl22SkePsuConfig.Builder().setOprpConfig(fileOprpConfig).setBcConfig(fileBcConfig).build()
        });

        // GMR21 (H3_SINGLETON_GCT OKVS, direct COT, NAIVE_3_HASH)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.GMR21.name() + " (H3_SINGLETON_GCT OKVS, direct COT, NAIVE_3_HASH)",
            new Gmr21PsuConfig.Builder().setOsnConfig(directCotOsnConfig).setOkvsType(OkvsType.H3_SINGLETON_GCT).build(),
        });
        // GMR21 (H2_SINGLETON_GCT OKVS, direct COT, NAIVE_3_HASH)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.GMR21.name() + " (H2_SINGLETON_GCT OKVS, direct COT, NAIVE_3_HASH)",
            new Gmr21PsuConfig.Builder().setOsnConfig(directCotOsnConfig).setOkvsType(OkvsType.H2_SINGLETON_GCT).build(),
        });
        // GMR21 (MEGA_BIN OKVS, silent COT, NAIVE_3_HASH)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.GMR21.name() + " (MEGA_BIN OKVS, silent COT, NAIVE_3_HASH)",
            new Gmr21PsuConfig.Builder().setOkvsType(OkvsType.MEGA_BIN).build(),
        });
        // GMR21 (MEGA_BIN OKVS, direct COT, NAIVE_3_HASH)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.GMR21.name() + " (MEGA_BIN OKVS, direct COT, NAIVE_3_HASH)",
            new Gmr21PsuConfig.Builder().setOsnConfig(directCotOsnConfig).setOkvsType(OkvsType.MEGA_BIN).build(),
        });
        // GMR21 (MEGA_BIN OKVS, direct COT, NAIVE_4_HASH)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.GMR21.name() + " (MEGA_BIN OKVS, direct COT, NAIVE_4_HASH)",
            new Gmr21PsuConfig.Builder()
                .setOsnConfig(directCotOsnConfig)
                .setOkvsType(OkvsType.MEGA_BIN)
                .setCuckooHashBinType(CuckooHashBinType.NAIVE_4_HASH)
                .build(),
        });
        // GMR21 (MEGA_BIN OKVS, direct COT, NO_STASH_PSZ18_3_HASH)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.GMR21.name() + " (MEGA_BIN OKVS, direct COT, NO_STASH_PSZ18_3_HASH)",
            new Gmr21PsuConfig.Builder()
                .setOsnConfig(directCotOsnConfig)
                .setOkvsType(OkvsType.MEGA_BIN)
                .setCuckooHashBinType(CuckooHashBinType.NO_STASH_PSZ18_3_HASH)
                .build(),
        });

        // KRTW19_OPT
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.KRTW19_OPT.name(),
            new Krtw19OptPsuConfig.Builder().build(),
        });

        // KRTW19_ORI (H3_SINGLETON_GCT OKVS)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.KRTW19_ORI.name() + " (H2_SINGLETON_GCT OKVS)",
            new Krtw19OriPsuConfig.Builder().setOkvsType(OkvsType.H3_SINGLETON_GCT).build(),
        });
        // KRTW19_ORI (H2_SINGLETON_GCT OKVS)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.KRTW19_ORI.name() + " (H2_SINGLETON_GCT OKVS)",
            new Krtw19OriPsuConfig.Builder().setOkvsType(OkvsType.H2_SINGLETON_GCT).build(),
        });
        // KRTW19_ORI (MEGA_BIN OKVS)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.KRTW19_ORI.name() + " (MEGA_BIN OKVS)",
            new Krtw19OriPsuConfig.Builder().setOkvsType(OkvsType.MEGA_BIN).build(),
        });
        // KRTW19_ORI (POLYNOMIAL OKVS)
        configurationParams.add(new Object[] {
            PsuFactory.PsuType.KRTW19_ORI.name() + " (POLYNOMIAL OKVS)",
            new Krtw19OriPsuConfig.Builder().setOkvsType(OkvsType.POLYNOMIAL).build(),
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
    private final PsuConfig config;

    public PsuTest(String name, PsuConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Test
    public void testPtoType() {
        PsuServer server = PsuFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsuClient client = PsuFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        Assert.assertEquals(config.getPtoType(), server.getPtoType());
        Assert.assertEquals(config.getPtoType(), client.getPtoType());
    }

    @Test
    public void test2() {
        PsuServer server = PsuFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsuClient client = PsuFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPto(server, client, 2, 2, DEFAULT_ELEMENT_BYTE_LENGTH);
    }

    @Test
    public void test10() {
        PsuServer server = PsuFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsuClient client = PsuFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPto(server, client, 10, 10, DEFAULT_ELEMENT_BYTE_LENGTH);
    }

    @Test
    public void testLargeServerSize() {
        PsuServer server = PsuFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsuClient client = PsuFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPto(server, client, DEFAULT_SIZE, 10, DEFAULT_ELEMENT_BYTE_LENGTH);
    }

    @Test
    public void testLargeClientSize() {
        PsuServer server = PsuFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsuClient client = PsuFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPto(server, client, 10, DEFAULT_SIZE, DEFAULT_ELEMENT_BYTE_LENGTH);
    }

    @Test
    public void testSmallElementByteLength() {
        PsuServer server = PsuFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsuClient client = PsuFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPto(server, client, DEFAULT_SIZE, DEFAULT_SIZE, SMALL_ELEMENT_BYTE_LENGTH);
    }

    @Test
    public void testLargeElementByteLength() {
        PsuServer server = PsuFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsuClient client = PsuFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPto(server, client, DEFAULT_SIZE, DEFAULT_SIZE, LARGE_ELEMENT_BYTE_LENGTH);
    }

    @Test
    public void testDefault() {
        PsuServer server = PsuFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsuClient client = PsuFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPto(server, client, DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_ELEMENT_BYTE_LENGTH);
    }

    @Test
    public void testParallelDefault() {
        PsuServer server = PsuFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsuClient client = PsuFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        server.setParallel(true);
        client.setParallel(true);
        testPto(server, client, DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_ELEMENT_BYTE_LENGTH);
    }

    @Test
    public void testLarge() {
        PsuServer server = PsuFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsuClient client = PsuFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        testPto(server, client, LARGE_SIZE, LARGE_SIZE, LARGE_ELEMENT_BYTE_LENGTH);
    }

    @Test
    public void testParallelLarge() {
        PsuServer server = PsuFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsuClient client = PsuFactory.createClient(clientRpc, serverRpc.ownParty(), config);
        server.setParallel(true);
        client.setParallel(true);
        testPto(server, client, LARGE_SIZE, LARGE_SIZE, LARGE_ELEMENT_BYTE_LENGTH);
    }

    private void testPto(PsuServer server, PsuClient client, int serverSize, int clientSize, int elementByteLength) {
        long randomTaskId = Math.abs(SECURE_RANDOM.nextLong());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，server_size = {}，client_size = {}-----",
                server.getPtoDesc().getPtoName(), serverSize, clientSize
            );
            // 生成集合
            ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSize, clientSize, elementByteLength);
            Set<ByteBuffer> serverSet = sets.get(0);
            Set<ByteBuffer> clientSet = sets.get(1);
            // 构建线程
            PsuServerThread serverThread = new PsuServerThread(server, serverSet, clientSet.size(), elementByteLength);
            PsuClientThread clientThread = new PsuClientThread(client, clientSet, serverSet.size(), elementByteLength);
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
            assertOutput(serverSet, clientSet, clientThread.getUnionSet());
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

    private void assertOutput(Set<ByteBuffer> serverSet, Set<ByteBuffer> clientSet, Set<ByteBuffer> clientUnionSet) {
        // 计算并集
        Set<ByteBuffer> union = new HashSet<>(serverSet);
        union.addAll(clientSet);
        Assert.assertTrue(clientUnionSet.containsAll(union));
        Assert.assertTrue(union.containsAll(clientUnionSet));
    }
}

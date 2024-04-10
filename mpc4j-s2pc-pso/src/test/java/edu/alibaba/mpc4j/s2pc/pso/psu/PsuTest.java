package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.psu.gmr21.Gmr21PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfcPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfsPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.krtw19.Krtw19PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl23.Zcl23PkePsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl23.Zcl23SkePsuConfig;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
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
public class PsuTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsuTest.class);
    /**
     * 默认数量
     */
    private static final int DEFAULT_SIZE = 99;
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
        Collection<Object[]> configurations = new ArrayList<>();

        // JSZ22_SFS (direct)
        configurations.add(new Object[]{
            PsuFactory.PsuType.JSZ22_SFS.name() + " (direct)",
            new Jsz22SfsPsuConfig.Builder(false).build(),
        });
        // JSZ22_SFS (silent)
        configurations.add(new Object[]{
            PsuFactory.PsuType.JSZ22_SFS.name() + " (silent)",
            new Jsz22SfsPsuConfig.Builder(true).build(),
        });
        // JSZ22_SFC (direct)
        configurations.add(new Object[]{
            PsuFactory.PsuType.JSZ22_SFC.name() + " (direct)",
            new Jsz22SfcPsuConfig.Builder(false).build(),
        });
        // JSZ22_SFC (silent)
        configurations.add(new Object[]{
            PsuFactory.PsuType.JSZ22_SFC.name() + " (silent)",
            new Jsz22SfcPsuConfig.Builder(true).build(),
        });
        // ZCL22_PKE
        configurations.add(new Object[]{
            PsuFactory.PsuType.ZCL23_PKE.name(),
            new Zcl23PkePsuConfig.Builder().build(),
        });
        // ZCL22_SKE
        configurations.add(new Object[]{
            PsuFactory.PsuType.ZCL23_SKE.name(),
            new Zcl23SkePsuConfig.Builder(SecurityModel.SEMI_HONEST).build(),
        });
        // GMR21 (direct)
        configurations.add(new Object[]{
            PsuFactory.PsuType.GMR21.name() + " (direct)",
            new Gmr21PsuConfig.Builder(false).build(),
        });
        // GMR21 (silent)
        configurations.add(new Object[]{
            PsuFactory.PsuType.GMR21.name() + " (silent)",
            new Gmr21PsuConfig.Builder(true).build(),
        });
        // KRTW19
        configurations.add(new Object[]{
            PsuFactory.PsuType.KRTW19.name(),
            new Krtw19PsuConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final PsuConfig config;

    public PsuTest(String name, PsuConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test2() {
        testPto(2, 2, DEFAULT_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void test10() {
        testPto(10, 10, DEFAULT_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testLargeServerSize() {
        testPto(DEFAULT_SIZE, 10, DEFAULT_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testLargeClientSize() {
        testPto(10, DEFAULT_SIZE, DEFAULT_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testSmallElementByteLength() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, SMALL_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testLargeElementByteLength() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, LARGE_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_ELEMENT_BYTE_LENGTH, true);
    }

    @Test
    public void testLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, LARGE_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, LARGE_ELEMENT_BYTE_LENGTH, true);
    }

    private void testPto(int serverSize, int clientSize, int elementByteLength, boolean parallel) {
        PsuServer server = PsuFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        PsuClient client = PsuFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，server_size = {}，client_size = {}-----",
                server.getPtoDesc().getPtoName(), serverSize, clientSize
            );
            // generate sets
            ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSize, clientSize, elementByteLength);
            Set<ByteBuffer> serverSet = sets.get(0);
            Set<ByteBuffer> clientSet = sets.get(1);
            PsuServerThread serverThread = new PsuServerThread(server, serverSet, clientSet.size(), elementByteLength);
            PsuClientThread clientThread = new PsuClientThread(client, clientSet, serverSet.size(), elementByteLength);
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
            assertOutput(serverSet, clientSet, clientThread.getUnionSet());
            printAndResetRpc(time);
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(Set<ByteBuffer> serverSet, Set<ByteBuffer> clientSet, Set<ByteBuffer> outputUnionSet) {
        // compute union
        Set<ByteBuffer> expectUnionSet = new HashSet<>(serverSet);
        expectUnionSet.addAll(clientSet);
        Assert.assertTrue(outputUnionSet.containsAll(expectUnionSet));
        Assert.assertTrue(expectUnionSet.containsAll(outputUnionSet));
    }
}

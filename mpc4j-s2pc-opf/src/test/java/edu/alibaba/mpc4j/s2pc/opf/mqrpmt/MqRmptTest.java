package edu.alibaba.mpc4j.s2pc.opf.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.opf.OpfUtils;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz22.Czz22ByteEccCwMqRpmtConfig;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * mqRPMT test.
 *
 * @author Weiran Liu
 * @date 2022/09/10
 */
@RunWith(Parameterized.class)
public class MqRmptTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqRmptTest.class);
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = 1000;
    /**
     * default element byte length
     */
    private static final int ELEMENT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * large size
     */
    private static final int LARGE_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // GMR21
        configurations.add(new Object[]{
            MqRpmtFactory.MqRpmtType.GMR21.name(),
            new Gmr21MqRpmtConfig.Builder(false).build(),
        });
        // CZZ22_BYTE_ECC_CW
        configurations.add(new Object[]{
            MqRpmtFactory.MqRpmtType.CZZ22_BYTE_ECC_CW.name(),
            new Czz22ByteEccCwMqRpmtConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final MqRpmtConfig config;

    public MqRmptTest(String name, MqRpmtConfig config) {
        super(name);
        this.config = config;
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
        MqRpmtServer server = MqRpmtFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        MqRpmtClient client = MqRpmtFactory.createClient(secondRpc, firstRpc.ownParty(), config);
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
            ArrayList<Set<ByteBuffer>> sets = OpfUtils.generateBytesSets(serverSize, clientSize, ELEMENT_BYTE_LENGTH);
            Set<ByteBuffer> serverSet = sets.get(0);
            Set<ByteBuffer> clientSet = sets.get(1);
            MqRpmtServerThread serverThread = new MqRpmtServerThread(server, serverSet, clientSet.size());
            MqRpmtClientThread clientThread = new MqRpmtClientThread(client, clientSet, serverSet.size());
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
            ByteBuffer[] serverVector = serverThread.getServerVector();
            boolean[] containVector = clientThread.getContainVector();
            assertOutput(serverVector, clientSet, containVector);
            printAndResetRpc(time);
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(ByteBuffer[] serverVector, Set<ByteBuffer> clientSet, boolean[] containVector) {
        Assert.assertEquals(serverVector.length, containVector.length);
        int vectorLength = serverVector.length;
        IntStream.range(0, vectorLength).forEach(index ->
            Assert.assertEquals(clientSet.contains(serverVector[index]), containVector[index])
        );
    }
}

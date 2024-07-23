package edu.alibaba.mpc4j.s2pc.opf.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.opf.OpfUtils;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory.MqRpmtType;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz24.Czz24CwOprfMqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.zcl23.Zcl23PkeMqRpmtConfig;
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

        configurations.add(new Object[]{
            MqRpmtType.ZCL23_PKE.name(), new Zcl23PkeMqRpmtConfig.Builder().build(),
        });
        // GMR21
        configurations.add(new Object[]{
            MqRpmtType.GMR21.name(), new Gmr21MqRpmtConfig.Builder(false).build(),
        });
        // CZZ24_BYTE_ECC_CW
        configurations.add(new Object[]{
            MqRpmtFactory.MqRpmtType.CZZ24_CW_OPRF.name(), new Czz24CwOprfMqRpmtConfig.Builder().build(),
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
        testPto(10, 15, false);
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

    private void testPto(int serverElementSize, int clientElementSize, boolean parallel) {
        MqRpmtServer server = MqRpmtFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        MqRpmtClient client = MqRpmtFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，server_size = {}，client_size = {}-----",
                server.getPtoDesc().getPtoName(), serverElementSize, clientElementSize
            );
            // generate sets
            ArrayList<Set<ByteBuffer>> sets = OpfUtils.generateBytesSets(serverElementSize, clientElementSize, ELEMENT_BYTE_LENGTH);
            Set<ByteBuffer> serverSet = sets.get(0);
            Set<ByteBuffer> clientSet = sets.get(1);
            MqRpmtServerThread serverThread = new MqRpmtServerThread(server, serverSet, clientSet.size());
            MqRpmtClientThread clientThread = new MqRpmtClientThread(client, clientSet, serverSet.size());
            // start
            STOP_WATCH.start();
            serverThread.start();
            clientThread.start();
            // stop
            serverThread.join();
            clientThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            // verify
            ByteBuffer[] serverVector = serverThread.getServerOutput();
            boolean[] containVector = clientThread.getClientOutput();
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

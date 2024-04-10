package edu.alibaba.mpc4j.s2pc.pso.payablepsi;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.payablepsi.zlp23.Zlp23PayablePsiConfig;
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
 * Payable PSI tests.
 *
 * @author Liqiang Peng
 * @date 2023/9/15
 */
@RunWith(Parameterized.class)
public class PayablePsiTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayablePsiTest.class);
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = 99;
    /**
     * element byte length
     */
    private static final int ELEMENT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * large size
     */
    private static final int LARGE_SIZE = 1 << 18;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // ZLP23
        configurations.add(new Object[] {
            PayablePsiFactory.PayablePsiType.ZLP23.name(),
            new Zlp23PayablePsiConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final PayablePsiConfig config;

    public PayablePsiTest(String name, PayablePsiConfig config) {
       super(name);
        this.config = config;
    }

    @Test
    public void test1() {
        testPto(1, 1, false);
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

    private void testPto(int serverSetSize, int clientSetSize, boolean parallel) {
        PayablePsiServer<ByteBuffer> server = PayablePsiFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        PayablePsiClient<ByteBuffer> client = PayablePsiFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，server_size = {}，client_size = {}-----",
                server.getPtoDesc().getPtoName(), serverSetSize, clientSetSize
            );
            // generate sets
            ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSetSize, clientSetSize, ELEMENT_BYTE_LENGTH);
            Set<ByteBuffer> serverSet = sets.get(0);
            Set<ByteBuffer> clientSet = sets.get(1);
            PayablePsiServerThread serverThread = new PayablePsiServerThread(server, serverSet, clientSet.size());
            PayablePsiClientThread clientThread = new PayablePsiClientThread(client, clientSet, serverSet.size());
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
            assertOutput(serverSet, clientSet, serverThread.getIntersectionSetSize(), clientThread.getIntersectionSet());
            printAndResetRpc(time);
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(Set<ByteBuffer> serverSet, Set<ByteBuffer> clientSet, int intersectionSetSize,
                              Set<ByteBuffer> outputIntersectionSet) {
        Set<ByteBuffer> expectIntersectionSet = new HashSet<>(serverSet);
        expectIntersectionSet.retainAll(clientSet);
        Assert.assertEquals(expectIntersectionSet.size(), intersectionSetSize);
        Assert.assertTrue(outputIntersectionSet.containsAll(expectIntersectionSet));
        Assert.assertTrue(expectIntersectionSet.containsAll(outputIntersectionSet));
    }
}

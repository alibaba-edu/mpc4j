package edu.alibaba.mpc4j.s2pc.pso.aidpsi;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.s2pc.pso.aidpsi.AidPsiFactory.AidPsiType;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.aidpsi.passive.Kmrs14ShAidPsiConfig;
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
 * aid PSI test.
 *
 * @author Weiran Liu
 * @date 2023/5/8
 */
@RunWith(Parameterized.class)
public class AidPsiTest extends AbstractThreePartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(AidPsiTest.class);
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = 99;
    /**
     * element byte length
     */
    private static final int ELEMENT_BYTE_LENGTH = 17;
    /**
     * large size
     */
    private static final int LARGE_SIZE = 1 << 20;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // KRMS14 (semi-honest)
        configurations.add(new Object[] {
            AidPsiType.KMRS14_SH_AIDER.name(), new Kmrs14ShAidPsiConfig.Builder().build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final AidPsiConfig config;

    public AidPsiTest(String name, AidPsiConfig config) {
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
        AidPsiParty<ByteBuffer> server = AidPsiFactory.createServer(
            firstRpc, secondRpc.ownParty(), thirdRpc.ownParty(), config
        );
        AidPsiParty<ByteBuffer> client = AidPsiFactory.createClient(
            secondRpc, firstRpc.ownParty(), thirdRpc.ownParty(), config
        );
        AidPsiAider aider = AidPsiFactory.createAider(
            thirdRpc, firstRpc.ownParty(), secondRpc.ownParty(), config
        );
        server.setParallel(parallel);
        client.setParallel(parallel);
        aider.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        aider.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，server_size = {}，client_size = {}-----",
                server.getPtoDesc().getPtoName(), serverSetSize, clientSetSize
            );
            ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSetSize, clientSetSize, ELEMENT_BYTE_LENGTH);
            Set<ByteBuffer> serverSet = sets.get(0);
            Set<ByteBuffer> clientSet = sets.get(1);
            AidPsiPartyThread serverThread = new AidPsiPartyThread(server, serverSet, clientSet.size());
            AidPsiPartyThread clientThread = new AidPsiPartyThread(client, clientSet, serverSet.size());
            AidPsiAiderThread aiderThread = new AidPsiAiderThread(aider, serverSet.size(), clientSet.size());
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            serverThread.start();
            clientThread.start();
            aiderThread.start();
            // stop
            serverThread.join();
            clientThread.join();
            aiderThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            assertOutput(serverSet, clientSet, clientThread.getIntersectionSet());
            printAndResetRpc(time);
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
            new Thread(aider::destroy).start();
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

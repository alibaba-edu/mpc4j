package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;
import edu.alibaba.mpc4j.s2pc.pso.psu.gmr21.Gmr21PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfcPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.jsz22.Jsz22SfsPsuConfig;
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
 * online-offline PSU test.
 *
 * @author Feng Han
 * @date 2024/12/9
 */
@RunWith(Parameterized.class)
public class OoPsuTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsuTest.class);
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = 99;
    /**
     * default element byte length
     */
    private static final int DEFAULT_ELEMENT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * small element byte length
     */
    private static final int SMALL_ELEMENT_BYTE_LENGTH = Long.BYTES;
    /**
     * large element byte length
     */
    private static final int LARGE_ELEMENT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH * 2;
    /**
     * large size
     */
    private static final int LARGE_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // JSZ22_SFS (direct)
        configurations.add(new Object[]{
            PsuType.JSZ22_SFS.name() + " (direct)", new Jsz22SfsPsuConfig.Builder(false).build(),
        });
        // JSZ22_SFC (direct)
        configurations.add(new Object[]{
            PsuType.JSZ22_SFC.name() + " (direct)", new Jsz22SfcPsuConfig.Builder(false).build(),
        });
        // GMR21 (direct)
        configurations.add(new Object[]{
            PsuType.GMR21.name() + " (direct)", new Gmr21PsuConfig.Builder(false).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final OoPsuConfig config;

    public OoPsuTest(String name, OoPsuConfig config) {
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
        OoPsuServer server = PsuFactory.createOoPsuServer(firstRpc, secondRpc.ownParty(), config);
        OoPsuClient client = PsuFactory.createOoPsuClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}, server_size = {}, client_size = {}-----",
                server.getPtoDesc().getPtoName(), serverSize, clientSize
            );
            // generate sets
            ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSize, clientSize, elementByteLength);
            Set<ByteBuffer> serverSet = sets.get(0);
            Set<ByteBuffer> clientSet = sets.get(1);
            OoPsuServerThread serverThread = new OoPsuServerThread(server, serverSet, clientSet.size(), elementByteLength);
            OoPsuClientThread clientThread = new OoPsuClientThread(client, clientSet, serverSet.size(), elementByteLength);
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
            assertOutput(serverSet, clientSet, clientThread.getClientOutput());
            printAndResetRpc(time);
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(Set<ByteBuffer> serverSet, Set<ByteBuffer> clientSet, PsuClientOutput clientOutput) {
        // compute intersection and union
        Set<ByteBuffer> expectIntersectionSet = new HashSet<>(serverSet);
        expectIntersectionSet.retainAll(clientSet);
        int expectPsiCa = expectIntersectionSet.size();
        Set<ByteBuffer> expectUnionSet = new HashSet<>(serverSet);
        expectUnionSet.addAll(clientSet);
        Assert.assertEquals(expectPsiCa, clientOutput.getPsiCa());
        Set<ByteBuffer> actualUnionSet = clientOutput.getUnion();
        Assert.assertTrue(actualUnionSet.containsAll(expectUnionSet));
        Assert.assertTrue(expectUnionSet.containsAll(actualUnionSet));
    }
}

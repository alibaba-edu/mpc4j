package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.aby.pcg.TrustDealer;
import edu.alibaba.mpc4j.s2pc.aby.pcg.TrustDealerThread;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;
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
 * PSU Z2 triple generation aid test.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
@RunWith(Parameterized.class)
public class PsuAidTest extends AbstractThreePartyMemoryRpcPto {
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

        // ZCL23_SKE
        configurations.add(new Object[]{
            PsuType.ZCL23_SKE.name() + "(" + SecurityModel.TRUSTED_DEALER + ")",
            new Zcl23SkePsuConfig.Builder(SecurityModel.TRUSTED_DEALER, true).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final PsuConfig config;

    public PsuAidTest(String name, PsuConfig config) {
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
        PsuServer server = PsuFactory.createServer(firstRpc, secondRpc.ownParty(), thirdRpc.ownParty(), config);
        PsuClient client = PsuFactory.createClient(secondRpc, firstRpc.ownParty(), thirdRpc.ownParty(), config);
        TrustDealer trustDealer = new TrustDealer(thirdRpc, firstRpc.ownParty(), secondRpc.ownParty());
        server.setParallel(parallel);
        client.setParallel(parallel);
        trustDealer.setParallel(parallel);
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
            TrustDealerThread trustDealerThread = new TrustDealerThread(trustDealer);
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            serverThread.start();
            clientThread.start();
            trustDealerThread.start();
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
            trustDealerThread.join();
            new Thread(trustDealer::destroy).start();
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

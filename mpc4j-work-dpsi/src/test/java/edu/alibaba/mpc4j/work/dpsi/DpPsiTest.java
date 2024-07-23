package edu.alibaba.mpc4j.work.dpsi;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.work.dpsi.DpsiFactory.DpPsiType;
import edu.alibaba.mpc4j.work.dpsi.ccpsi.CcpsiDpsiConfig;
import edu.alibaba.mpc4j.work.dpsi.mqrpmt.MqRpmtDpsiConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory.MqRpmtType;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz24.Czz24CwOprfMqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory.CcpsiType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.cgs22.Cgs22CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.psty19.Psty19CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.rs21.Rs21CcpsiConfig;
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

/**
 * DP-PSI test.
 *
 * @author Yufei Wang
 * @date 2023/9/19
 */
@RunWith(Parameterized.class)
public class DpPsiTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(DpPsiTest.class);
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
    private static final int LARGE_SIZE = 1 << 14;
    /**
     * privacy budget
     */
    private static final double EPSILON = 2.0;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // MQ_RPMT_BASED (CZZ24_CW_OPRF)
        configurations.add(new Object[]{
            DpPsiType.MQ_RPMT_BASED.name() + " (" + MqRpmtType.CZZ24_CW_OPRF.name() + ")",
            new MqRpmtDpsiConfig.Builder(EPSILON, EPSILON / 2, EPSILON / 2)
                .setMqRpmtConfig(new Czz24CwOprfMqRpmtConfig.Builder().build())
                .build(),
        });
        // MQ_RPMT_BASED (GMR21)
        configurations.add(new Object[]{
            DpPsiType.MQ_RPMT_BASED.name() + " (" + MqRpmtType.GMR21.name() + ")",
            new MqRpmtDpsiConfig.Builder(EPSILON, EPSILON / 2, EPSILON / 2)
                .setMqRpmtConfig(new Gmr21MqRpmtConfig.Builder(false).build())
                .build(),
        });
        // CCPSI_BASED (CGS22)
        configurations.add(new Object[]{
            DpPsiType.CCPSI_BASED.name() + " (" + CcpsiType.CGS22.name() + ")",
            new CcpsiDpsiConfig.Builder(EPSILON)
                .setCcpsiConfig(new Cgs22CcpsiConfig.Builder(true).build())
                .build(),
        });
        // CCPSI_BASED (PSTY19)
        configurations.add(new Object[]{
            DpPsiType.CCPSI_BASED.name() + " (" + CcpsiType.PSTY19.name() + ")",
            new CcpsiDpsiConfig.Builder(EPSILON)
                .setCcpsiConfig(new Psty19CcpsiConfig.Builder(true).build())
                .build(),
        });
        // CCPSI_BASED (RS21)
        configurations.add(new Object[]{
            DpPsiType.CCPSI_BASED.name() + " (" + CcpsiType.RS21.name() + ")",
            new CcpsiDpsiConfig.Builder(EPSILON)
                .setCcpsiConfig(new Rs21CcpsiConfig.Builder(true).build())
                .build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final DpsiConfig config;

    public DpPsiTest(String name, DpsiConfig config) {
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

    private void testPto(int serverSetSize, int clientSetSize, boolean parallel) {
        DpsiServer<ByteBuffer> server = DpsiFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        DpsiClient<ByteBuffer> client = DpsiFactory.createClient(secondRpc, firstRpc.ownParty(), config);
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
            DpPsiServerThread serverThread = new DpPsiServerThread(server, serverSet, clientSet.size());
            DpPsiClientThread clientThread = new DpPsiClientThread(client, clientSet, serverSet.size());
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

    private void assertOutput(Set<ByteBuffer> serverElementSet, Set<ByteBuffer> clientElementSet,
                                  Set<ByteBuffer> actualIntersection) {
        // it is hard to verify the result, we only know that the intersection should be the subset of client set.
        Assert.assertTrue(actualIntersection.size() <= clientElementSet.size());
        Assert.assertTrue(clientElementSet.containsAll(actualIntersection));
        // compute some measurements.
        int tp = 0;
        int fp = 0;
        int tn = 0;
        int fn = 0;
        Set<ByteBuffer> expectIntersection = new HashSet<>(serverElementSet);
        expectIntersection.retainAll(clientElementSet);
        for (ByteBuffer element : clientElementSet) {
            if ((actualIntersection.contains(element)) && (expectIntersection.contains(element))) {
                tp = tp + 1;
            } else if (!(actualIntersection.contains(element)) && (expectIntersection.contains(element))) {
                fn = fn + 1;
            } else if ((actualIntersection.contains(element)) && !(expectIntersection.contains(element))) {
                fp = fp + 1;
            } else if (!(actualIntersection.contains(element)) && !(expectIntersection.contains(element))) {
                tn = tn + 1;
            }
        }
        LOGGER.info("TP = {}, FP = {}, TN = {}, FN = {}", tp, fp, tn, fn);
    }
}

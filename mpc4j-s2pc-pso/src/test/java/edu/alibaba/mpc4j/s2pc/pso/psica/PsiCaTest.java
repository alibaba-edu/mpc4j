package edu.alibaba.mpc4j.s2pc.pso.psica;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.psica.cgt12.Cgt12EccPsiCaConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.ccpsi.CcPsiCaConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.gmr21.Gmr21PsiCaConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.hfh99.Hfh99EccPsiCaConfig;
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
 * PSI Cardinality test.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
@RunWith(Parameterized.class)
public class PsiCaTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(edu.alibaba.mpc4j.s2pc.pso.psi.PsiTest.class);
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
    private static final int LARGE_SIZE = 1 << 14;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // HFH99_ECC (compress)
        configurations.add(new Object[]{
            PsiCaFactory.PsiCaType.HFH99_ECC.name() + " (compress)",
            new Hfh99EccPsiCaConfig.Builder().setCompressEncode(true).build(),
        });
        // HFH99_ECC (uncompress)
        configurations.add(new Object[]{
            PsiCaFactory.PsiCaType.HFH99_ECC.name() + " (uncompress)",
            new Hfh99EccPsiCaConfig.Builder().setCompressEncode(false).build(),
        });
        // CGT12_ECC (compress)
        configurations.add(new Object[]{
            PsiCaFactory.PsiCaType.CGT12_ECC.name() + " (compress)",
            new Cgt12EccPsiCaConfig.Builder().setCompressEncode(true).build(),
        });
        // CGT12_ECC (uncompress)
        configurations.add(new Object[]{
            PsiCaFactory.PsiCaType.CGT12_ECC.name() + " (uncompress)",
            new Cgt12EccPsiCaConfig.Builder().setCompressEncode(false).build(),
        });
        // client-payload circuit PSI (direct)
        configurations.add(new Object[]{
            PsiCaFactory.PsiCaType.CCPSI.name() + " (direct)",
            new CcPsiCaConfig.Builder(false).build(),
        });
        // client-payload circuit PSI (silent)
        configurations.add(new Object[]{
            PsiCaFactory.PsiCaType.CCPSI.name() + " (silent)",
            new CcPsiCaConfig.Builder(true).build(),
        });
        // GMR21
        configurations.add(new Object[]{
            PsiCaFactory.PsiCaType.GMR21.name() + " (silent)",
            new Gmr21PsiCaConfig.Builder(false).build(),
        });
        return configurations;
    }

    /**
     * config
     */
    private final PsiCaConfig config;

    public PsiCaTest(String name, PsiCaConfig config) {
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

    private void testPto(int serverSize, int clientSize, boolean parallel) {
        PsiCaServer<ByteBuffer> server = PsiCaFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        PsiCaClient<ByteBuffer> client = PsiCaFactory.createClient(secondRpc, firstRpc.ownParty(), config);
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
            ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSize, clientSize, ELEMENT_BYTE_LENGTH);
            Set<ByteBuffer> serverSet = sets.get(0);
            Set<ByteBuffer> clientSet = sets.get(1);
            PsiCaServerThread serverThread = new PsiCaServerThread(server, serverSet, clientSet.size());
            PsiCaClientThread clientThread = new PsiCaClientThread(client, clientSet, serverSet.size());
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
            assertOutput(serverSet, clientSet, clientThread.getCardinality());
            printAndResetRpc(time);
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(Set<ByteBuffer> serverSet, Set<ByteBuffer> clientSet, int cardinality) {
        Set<ByteBuffer> intersectionSet = new HashSet<>(serverSet);
        intersectionSet.retainAll(clientSet);
        int expectedCardinality = intersectionSet.size();

        Assert.assertEquals(expectedCardinality, cardinality);
    }
}
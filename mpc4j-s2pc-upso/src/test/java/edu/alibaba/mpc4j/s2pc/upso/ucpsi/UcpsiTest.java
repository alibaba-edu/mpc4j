package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.pdsm.Sj23PdsmUcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt.Sj23PeqtUcpsiConfig;
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
 * unbalanced circuit PSI test.
 *
 * @author Liqiang Peng
 * @date 2023/4/18
 */
@RunWith(Parameterized.class)
public class UcpsiTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(UcpsiTest.class);
    /**
     * default server element size
     */
    private static final int DEFAULT_SERVER_ELEMENT_SIZE = 1 << 16;
    /**
     * default client element size
     */
    private static final int DEFAULT_CLIENT_ELEMENT_SIZE = 1 << 6;
    /**
     * default client element size
     */
    private static final int SMALL_CLIENT_ELEMENT_SIZE = 1 << 4;
    /**
     * element bit length
     */
    private static final int ELEMENT_BIT_LENGTH = Double.SIZE;
    /**
     * element byte length
     */
    private static final int ELEMENT_BYTE_LENGTH = CommonUtils.getByteLength(ELEMENT_BIT_LENGTH);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // SJ23
        configurations.add(new Object[]{
            UcpsiFactory.UcpsiType.SJ23_PEQT.name(),
            new Sj23PeqtUcpsiConfig.Builder(SecurityModel.SEMI_HONEST, true)
                .build()
        });
        configurations.add(new Object[]{
            UcpsiFactory.UcpsiType.SJ23_PDSM.name(),
            new Sj23PdsmUcpsiConfig.Builder(SecurityModel.SEMI_HONEST, true)
                .build()
        });
        // PSTY19
        configurations.add(new Object[]{
            UcpsiFactory.UcpsiType.PSTY19.name() + " (silent)",
            new Psty19UcpsiConfig.Builder(SecurityModel.SEMI_HONEST, true).build()
        });
        configurations.add(new Object[]{
            UcpsiFactory.UcpsiType.PSTY19.name() + " (direct)",
            new Psty19UcpsiConfig.Builder(SecurityModel.SEMI_HONEST, false).build()
        });
        return configurations;
    }

    /**
     * unbalanced PSI config
     */
    private final UcpsiConfig config;

    public UcpsiTest(String name, UcpsiConfig config) {
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
    public void testDefault() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, DEFAULT_CLIENT_ELEMENT_SIZE, false);
    }

    @Test
    public void testDefaultParallel() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, DEFAULT_CLIENT_ELEMENT_SIZE, true);
    }

    @Test
    public void testSmallClient() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, SMALL_CLIENT_ELEMENT_SIZE, true);
    }

    private void testPto(int serverSetSize, int clientSetSize, boolean parallel) {
        UcpsiServer<ByteBuffer> server = UcpsiFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        UcpsiClient<ByteBuffer> client = UcpsiFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，server_set_size = {}，client_set_size = {}-----",
                server.getPtoDesc().getPtoName(), serverSetSize, clientSetSize
            );
            // generate the inputs
            List<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSetSize, clientSetSize, ELEMENT_BYTE_LENGTH);
            Set<ByteBuffer> serverElementSet = sets.get(0);
            Set<ByteBuffer> clientElementSet = sets.get(1);
            UcpsiServerThread serverThread = new UcpsiServerThread(server, serverElementSet, clientSetSize);
            UcpsiClientThread clientThread = new UcpsiClientThread(client, clientElementSet, serverSetSize);
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
            SquareZ2Vector serverOutput = serverThread.getServerOutput();
            UcpsiClientOutput<ByteBuffer> clientOutput = clientThread.getClientOutput();
            assertOutput(serverElementSet, clientElementSet, serverOutput, clientOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(Set<ByteBuffer> serverElementSet, Set<ByteBuffer> clientElementSet,
                              SquareZ2Vector serverOutput, UcpsiClientOutput<ByteBuffer> clientOutput) {
        Set<ByteBuffer> expectIntersectionSet = new HashSet<>(serverElementSet);
        expectIntersectionSet.retainAll(clientElementSet);
        ArrayList<ByteBuffer> table = clientOutput.getTable();
        BitVector z = serverOutput.getBitVector().xor(clientOutput.getZ1().getBitVector());
        int beta = clientOutput.getBeta();
        for (int i = 0; i < beta; i++) {
            if (table.get(i) == null) {
                Assert.assertFalse(z.get(i));
            } else if (expectIntersectionSet.contains(table.get(i))) {
                Assert.assertTrue(z.get(i));
            } else {
                Assert.assertFalse(z.get(i));
            }
        }
    }
}

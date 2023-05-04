package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.cgs22.Cgs22UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.pir.PirUbopprfConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.pir.PirUrbopprfConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * unbalanced circuit PSI test.
 *
 * @author Liqiang Peng
 * @date 2023/4/18
 */
@RunWith(Parameterized.class)
public class UcpsiTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UcpsiTest.class);
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * default server element size
     */
    private static final int DEFAULT_SERVER_ELEMENT_SIZE = 1 << 15;
    /**
     * small server element size
     */
    private static final int SMALL_SERVER_ELEMENT_SIZE = 1 << 12;
    /**
     * default client element size
     */
    private static final int DEFAULT_CLIENT_ELEMENT_SIZE = 1 << 8;
    /**
     * element bit length
     */
    private static final int ELEMENT_BIT_LENGTH = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * element byte length
     */
    private static final int ELEMENT_BYTE_LENGTH = CommonUtils.getByteLength(ELEMENT_BIT_LENGTH);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CGS22
        configurations.add(new Object[]{
            UcpsiFactory.UcpsiType.CGS22.name() + " (direct + pir)",
            new Cgs22UcpsiConfig.Builder(false)
                .setUrbopprfConfig(new PirUrbopprfConfig.Builder().build())
                .build()
        });
        configurations.add(new Object[]{
            UcpsiFactory.UcpsiType.CGS22.name() + " (silent)", new Cgs22UcpsiConfig.Builder(true).build()
        });
        configurations.add(new Object[]{
            UcpsiFactory.UcpsiType.CGS22.name() + " (direct)", new Cgs22UcpsiConfig.Builder(false).build()
        });
        // PSTY19
        configurations.add(new Object[]{
            UcpsiFactory.UcpsiType.PSTY19.name() + " (direct + pir)",
            new Psty19UcpsiConfig.Builder(false)
                .setUbopprfConfig(new PirUbopprfConfig.Builder().build())
                .build()
        });
        configurations.add(new Object[]{
            UcpsiFactory.UcpsiType.PSTY19.name() + " (silent)", new Psty19UcpsiConfig.Builder(true).build()
        });
        configurations.add(new Object[]{
            UcpsiFactory.UcpsiType.PSTY19.name() + " (direct)", new Psty19UcpsiConfig.Builder(false).build()
        });
        return configurations;
    }

    /**
     * server rpc
     */
    private final Rpc serverRpc;
    /**
     * client rpc
     */
    private final Rpc clientRpc;
    /**
     * unbalanced PSI config
     */
    private final UcpsiConfig config;

    public UcpsiTest(String name, UcpsiConfig config) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.config = config;
    }

    @Before
    public void connect() {
        serverRpc.connect();
        clientRpc.connect();
    }

    @After
    public void disconnect() {
        serverRpc.disconnect();
        clientRpc.disconnect();
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
    public void testSmallServer() {
        testPto(SMALL_SERVER_ELEMENT_SIZE, DEFAULT_CLIENT_ELEMENT_SIZE, false);
    }

    private void testPto(int serverSetSize, int clientSetSize, boolean parallel) {
        UcpsiServer server = UcpsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        UcpsiClient client = UcpsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);
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
            ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSetSize, clientSetSize, ELEMENT_BYTE_LENGTH);
            Set<ByteBuffer> serverElementSet = sets.get(0);
            Set<ByteBuffer> clientElementSet = sets.get(1);
            UcpsiServerThread serverThread = new UcpsiServerThread(server, serverElementSet, clientSetSize);
            UcpsiClientThread clientThread = new UcpsiClientThread(client, clientElementSet, serverSetSize);
            StopWatch stopWatch = new StopWatch();
            // execute the protocol
            stopWatch.start();
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            SquareZ2Vector serverOutput = serverThread.getServerOutput();
            UcpsiClientOutput clientOutput = clientThread.getClientOutput();
            assertOutput(serverElementSet, clientElementSet, serverOutput, clientOutput);
            LOGGER.info("Server data_packet_num = {}, payload_bytes = {}B, send_bytes = {}B, time = {}ms",
                serverRpc.getSendDataPacketNum(), serverRpc.getPayloadByteLength(), serverRpc.getSendByteLength(),
                time
            );
            LOGGER.info("Client data_packet_num = {}, payload_bytes = {}B, send_bytes = {}B, time = {}ms",
                clientRpc.getSendDataPacketNum(), clientRpc.getPayloadByteLength(), clientRpc.getSendByteLength(),
                time
            );
            serverRpc.reset();
            clientRpc.reset();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }

    private void assertOutput(Set<ByteBuffer> serverElementSet, Set<ByteBuffer> clientElementSet,
                              SquareZ2Vector serverOutput, UcpsiClientOutput clientOutput) {
        Set<ByteBuffer> expectIntersectionSet = new HashSet<>(serverElementSet);
        expectIntersectionSet.retainAll(clientElementSet);
        ByteBuffer[] table = clientOutput.getTable();
        BitVector z = serverOutput.getBitVector().xor(clientOutput.getZ1().getBitVector());
        int beta = clientOutput.getBeta();
        for (int i = 0; i < beta; i++) {
            if (expectIntersectionSet.contains(table[i])) {
                Assert.assertTrue(z.get(i));
            } else {
                Assert.assertFalse(z.get(i));
            }
        }
    }
}

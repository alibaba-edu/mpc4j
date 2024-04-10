package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.cgs22.Cgs22ScpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.psty19.Psty19ScpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.ScpsiFactory.ScpsiType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.rs21.Rs21ScpsiConfig;
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
 * server-payload circuit PSI test.
 *
 * @author Liqiang Peng
 * @date 2023/1/30
 */
@RunWith(Parameterized.class)
public class ScpsiTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScpsiTest.class);
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = 99;
    /**
     * 较大数量
     */
    private static final int LARGE_SIZE = 1 << 14;
    /**
     * element byte length
     */
    private static final int ELEMENT_BIT_LENGTH = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * element byte length
     */
    private static final int ELEMENT_BYTE_LENGTH = CommonUtils.getByteLength(ELEMENT_BIT_LENGTH);
    /**
     * cuckoo hash bin types
     */
    private static final CuckooHashBinType[] CUCKOO_HASH_BIN_TYPES = new CuckooHashBinType[] {
        CuckooHashBinType.NO_STASH_PSZ18_3_HASH,
        CuckooHashBinType.NAIVE_2_HASH,
        CuckooHashBinType.NAIVE_4_HASH,
    };

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // RS21
        for (CuckooHashBinType type : CUCKOO_HASH_BIN_TYPES) {
            configurations.add(new Object[]{
                ScpsiType.RS21.name() + " (silent, " + type.name() + ")",
                new Rs21ScpsiConfig.Builder(true).setCuckooHashBinType(type).build(),
            });
            configurations.add(new Object[]{
                ScpsiType.RS21.name() + " (direct, " + type.name() + ")",
                new Rs21ScpsiConfig.Builder(false).setCuckooHashBinType(type).build(),
            });
        }
        // CGS22
        for (CuckooHashBinType type : CUCKOO_HASH_BIN_TYPES) {
            configurations.add(new Object[]{
                ScpsiType.CGS22.name() + " (silent, " + type.name() + ")",
                new Cgs22ScpsiConfig.Builder(true).setCuckooHashBinType(type).build(),
            });
            configurations.add(new Object[]{
                ScpsiType.CGS22.name() + " (direct, " + type.name() + ")",
                new Cgs22ScpsiConfig.Builder(false).setCuckooHashBinType(type).build(),
            });
        }
        // PSTY19
        for (CuckooHashBinType type : CUCKOO_HASH_BIN_TYPES) {
            configurations.add(new Object[]{
                ScpsiType.PSTY19.name() + " (silent, " + type.name() + ")",
                new Psty19ScpsiConfig.Builder(true).setCuckooHashBinType(type).build(),
            });
            configurations.add(new Object[]{
                ScpsiType.PSTY19.name() + " (direct, " + type.name() + ")",
                new Psty19ScpsiConfig.Builder(false).setCuckooHashBinType(type).build(),
            });
        }

        return configurations;
    }

    /**
     * the config
     */
    private final ScpsiConfig config;

    public ScpsiTest(String name, ScpsiConfig config) {
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
    public void testDefault() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, true);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, true);
    }

    @Test
    public void testLargeServerSize() {
        testPto(LARGE_SIZE, DEFAULT_SIZE, false);
    }

    @Test
    public void testLargeClientSize() {
        testPto(DEFAULT_SIZE, LARGE_SIZE, false);
    }

    @Test
    public void testLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, true);
    }

    public void testPto(int serverSetSize, int clientSetSize, boolean parallel) {
        ScpsiServer<ByteBuffer> server = ScpsiFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        ScpsiClient<ByteBuffer> client = ScpsiFactory.createClient(secondRpc, firstRpc.ownParty(), config);
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
            ScpsiServerThread serverThread = new ScpsiServerThread(server, serverElementSet, clientSetSize);
            ScpsiClientThread clientThread = new ScpsiClientThread(client, clientElementSet, serverSetSize);
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
            ScpsiServerOutput<ByteBuffer> serverOutput = serverThread.getServerOutput();
            SquareZ2Vector clientOutput = clientThread.getClientOutput();
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
                              ScpsiServerOutput<ByteBuffer> serverOutput, SquareZ2Vector clientOutput) {
        Set<ByteBuffer> expectIntersectionSet = new HashSet<>(serverElementSet);
        expectIntersectionSet.retainAll(clientElementSet);
        ArrayList<ByteBuffer> table = serverOutput.getTable();
        BitVector z = serverOutput.getZ0().getBitVector().xor(clientOutput.getBitVector());
        int beta = serverOutput.getBeta();
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

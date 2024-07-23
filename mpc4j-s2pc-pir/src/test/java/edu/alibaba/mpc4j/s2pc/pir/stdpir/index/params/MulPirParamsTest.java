package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.params;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirClientThread;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirServerThread;

import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.mul.MulStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.mul.MulStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.mul.MulStdIdxPirParams;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.mul.MulStdIdxPirServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Mul PIR params test.
 *
 * @author Liqiang Peng
 * @date 2024/7/15
 */
@RunWith(Parameterized.class)
public class MulPirParamsTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * default element bit length
     */
    private static final int DEFAULT_ELEMENT_BIT_LENGTH = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * large element bit length
     */
    private static final int LARGE_ELEMENT_BIT_LENGTH = 160000;
    /**
     * small element bit length
     */
    private static final int SMALL_ELEMENT_BIT_LENGTH = CommonConstants.STATS_BIT_LENGTH;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Mul PIR (1-dimension)
        configurations.add(new Object[]{
            StdIdxPirFactory.StdIdxPirType.MUL.name() + " (1-dimension)",
            new MulStdIdxPirConfig.Builder().setParams(new MulStdIdxPirParams(4096, 20, 1)).build()
        });
        // Mul PIR (2-dimension)
        configurations.add(new Object[]{
            StdIdxPirFactory.StdIdxPirType.MUL.name() + " (2-dimension)",
            new MulStdIdxPirConfig.Builder().setParams(new MulStdIdxPirParams(8192, 20, 2)).build()
        });
        // Mul PIR (3-dimension)
        configurations.add(new Object[]{
            StdIdxPirFactory.StdIdxPirType.MUL.name() + " (3-dimension)",
            new MulStdIdxPirConfig.Builder().setParams(new MulStdIdxPirParams(8192, 20, 3)).build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final MulStdIdxPirConfig config;
    /**
     * database size
     */
    private final int n;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public MulPirParamsTest(String name, MulStdIdxPirConfig config) {
        super(name);
        this.config = config;
        n = 1 << 12;
        secureRandom = new SecureRandom();
    }

    @Test
    public void testMulPir() {
        testPto(DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelMulPir() {
        testPto(DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementMulPir() {
        testPto(LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementMulPir() {
        testPto(SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testPto(int l, boolean parallel) {
        NaiveDatabase database = NaiveDatabase.createRandom(l, n, secureRandom);
        MulStdIdxPirServer server = new MulStdIdxPirServer(firstRpc, secondRpc.ownParty(), config);
        MulStdIdxPirClient client = new MulStdIdxPirClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        StdIdxPirServerThread serverThread = new StdIdxPirServerThread(server, database, 1, false);
        StdIdxPirClientThread clientThread = new StdIdxPirClientThread(client, n, l, 1, false);
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            // verify result
            Assert.assertTrue(serverThread.getSuccess());
            Assert.assertTrue(clientThread.getSuccess());
            int x = clientThread.getXs()[0];
            byte[] expect = database.getBytesData(x);
            byte[] actual = clientThread.getEntries()[0];
            Assert.assertArrayEquals(expect, actual);
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

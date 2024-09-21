package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.params;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirClientThread;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirServerThread;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j.Seal4jStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j.Seal4jStdIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j.Seal4jStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j.Seal4jStdIdxPirParams;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * SEAL PIR params test.
 *
 * @author Liqiang Peng
 * @date 2024/7/15
 */
@RunWith(Parameterized.class)
public class Seal4jPirParamsTest extends AbstractTwoPartyMemoryRpcPto {
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

        // SEAL4J PIR (1-dimension)
        configurations.add(new Object[]{
                StdIdxPirFactory.StdIdxPirType.SEAL4J.name() + " (1-dimension)",
                new Seal4jStdIdxPirConfig.Builder().setParams(new Seal4jStdIdxPirParams(4096, 20, 1)).build()
        });
        configurations.add(new Object[]{
                StdIdxPirFactory.StdIdxPirType.SEAL4J.name() + " (1-dimension)",
                new Seal4jStdIdxPirConfig.Builder().setParams(new Seal4jStdIdxPirParams(8192, 20, 1)).build()
        });
        // SEAL4J PIR (2-dimension)
        configurations.add(new Object[]{
                StdIdxPirFactory.StdIdxPirType.SEAL4J.name() + " (2-dimension)",
                new Seal4jStdIdxPirConfig.Builder().setParams(new Seal4jStdIdxPirParams(4096, 20, 2)).build()
        });
        configurations.add(new Object[]{
                StdIdxPirFactory.StdIdxPirType.SEAL4J.name() + " (2-dimension)",
                new Seal4jStdIdxPirConfig.Builder().setParams(new Seal4jStdIdxPirParams(8192, 20, 2)).build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final Seal4jStdIdxPirConfig config;
    /**
     * database size
     */
    private final int n;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public Seal4jPirParamsTest(String name, Seal4jStdIdxPirConfig config) {
        super(name);
        this.config = config;
        n = 1 << 12;
        secureRandom = new SecureRandom();
    }

    @Test
    public void testSealPir() {
        testPto(DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelSealPir() {
        testPto(DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementSealPir() {
        testPto(LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementSealPir() {
        testPto(SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testPto(int l, boolean parallel) {
        NaiveDatabase database = NaiveDatabase.createRandom(l, n, secureRandom);
        Seal4jStdIdxPirServer server = new Seal4jStdIdxPirServer(firstRpc, secondRpc.ownParty(), config);
        Seal4jStdIdxPirClient client = new Seal4jStdIdxPirClient(secondRpc, firstRpc.ownParty(), config);
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
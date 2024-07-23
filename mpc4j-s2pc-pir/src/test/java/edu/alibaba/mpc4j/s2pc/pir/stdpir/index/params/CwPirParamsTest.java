package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.params;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirClientThread;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirServerThread;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.cw.CwStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.cw.CwStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.cw.CwStdIdxPirParams;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.cw.CwStdIdxPirServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Constant weight PIR params test.
 *
 * @author Liqiang Peng
 * @date 2024/7/15
 */
@RunWith(Parameterized.class)
public class CwPirParamsTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * default element bit length
     */
    private static final int DEFAULT_ELEMENT_BIT_LENGTH = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * large element bit length
     */
    private static final int LARGE_ELEMENT_BIT_LENGTH = 10000;
    /**
     * small element bit length
     */
    private static final int SMALL_ELEMENT_BIT_LENGTH = CommonConstants.STATS_BIT_LENGTH;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            StdIdxPirFactory.StdIdxPirType.CW.name() + " (CONSTANT_WEIGHT_EQ)",
            new CwStdIdxPirConfig.Builder()
                .setParams(new CwStdIdxPirParams(2, 16384, 21, CwStdIdxPirParams.EqualityType.CONSTANT_WEIGHT))
                .build()
        });

        configurations.add(new Object[]{
            StdIdxPirFactory.StdIdxPirType.CW.name() + " (FOLKLORE)",
            new CwStdIdxPirConfig.Builder()
                .setParams(new CwStdIdxPirParams(2, 16384, 21, CwStdIdxPirParams.EqualityType.FOLKLORE))
                .build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final CwStdIdxPirConfig config;
    /**
     * database size
     */
    private final int n;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public CwPirParamsTest(String name, CwStdIdxPirConfig config) {
        super(name);
        this.config = config;
        n = 1 << 12;
        secureRandom = new SecureRandom();
    }

    @Test
    public void testCwPir() {
        testPto(DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelCwPir() {
        testPto(DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementCwPir() {
        testPto(LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementCwPir() {
        testPto(SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testPto(int l, boolean parallel) {
        NaiveDatabase database = NaiveDatabase.createRandom(l, n, secureRandom);
        CwStdIdxPirServer server = new CwStdIdxPirServer(firstRpc, secondRpc.ownParty(), config);
        CwStdIdxPirClient client = new CwStdIdxPirClient(secondRpc, firstRpc.ownParty(), config);
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



package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.params;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirClientThread;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory.StdIdxPirType;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirServerThread;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.xpir.XpirStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.xpir.XpirStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.xpir.XpirStdIdxPirParams;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.xpir.XpirStdIdxPirServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * XPIR params test.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
@RunWith(Parameterized.class)
public class XpirParamsTest extends AbstractTwoPartyMemoryRpcPto {
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

        // XPIR (1-dimension)
        configurations.add(new Object[]{
            StdIdxPirType.XPIR.name() + " (1-dimension)",
            new XpirStdIdxPirConfig.Builder().setParams(new XpirStdIdxPirParams(4096, 20, 1)).build()
        });
        configurations.add(new Object[]{
            StdIdxPirType.XPIR.name() + " (1-dimension)",
            new XpirStdIdxPirConfig.Builder().setParams(new XpirStdIdxPirParams(8192, 20, 1)).build()
        });
        // XPIR (2-dimension)
        configurations.add(new Object[]{
            StdIdxPirType.XPIR.name() + " (2-dimension)",
            new XpirStdIdxPirConfig.Builder().setParams(new XpirStdIdxPirParams(4096, 20, 2)).build()
        });
        configurations.add(new Object[]{
            StdIdxPirType.XPIR.name() + " (2-dimension)",
            new XpirStdIdxPirConfig.Builder().setParams(new XpirStdIdxPirParams(8192, 20, 2)).build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final XpirStdIdxPirConfig config;
    /**
     * database size
     */
    private final int n;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public XpirParamsTest(String name, XpirStdIdxPirConfig config) {
        super(name);
        this.config = config;
        n = 1 << 12;
        secureRandom = new SecureRandom();
    }

    @Test
    public void testXPir() {
        testPto(DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelXPir() {
        testPto(DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementXPir() {
        testPto(LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementXPir() {
        testPto(SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testPto(int l, boolean parallel) {
        NaiveDatabase database = NaiveDatabase.createRandom(l, n, secureRandom);
        XpirStdIdxPirServer server = new XpirStdIdxPirServer(firstRpc, secondRpc.ownParty(), config);
        XpirStdIdxPirClient client = new XpirStdIdxPirClient(secondRpc, firstRpc.ownParty(), config);
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
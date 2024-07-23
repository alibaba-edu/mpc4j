package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.params;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirClientThread;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory.StdIdxPirType;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirServerThread;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.cw.CwStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.fast.FastStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.mul.MulStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.onion.OnionStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.pbc.PbcStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.pbc.PbcStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.pbc.PbcStdIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal.SealStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.xpir.XpirStdIdxPirConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Cuckoo Hash based batch index PIR test.
 *
 * @author Liqiang Peng
 * @date 2024/7/17
 */
@RunWith(Parameterized.class)
public class PbcPirParamsTest extends AbstractTwoPartyMemoryRpcPto {
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
            StdIdxPirFactory.StdIdxPirType.PBC.name() + " " + StdIdxPirType.CW,
            new PbcStdIdxPirConfig.Builder()
                .setPbcStdIdxPirConfig(new CwStdIdxPirConfig.Builder().build())
                .build()
        });

        configurations.add(new Object[]{
            StdIdxPirFactory.StdIdxPirType.PBC.name() + " " + StdIdxPirType.SEAL,
            new PbcStdIdxPirConfig.Builder()
                .setPbcStdIdxPirConfig(new SealStdIdxPirConfig.Builder().build())
                .build()
        });

        configurations.add(new Object[]{
            StdIdxPirFactory.StdIdxPirType.PBC.name() + " " + StdIdxPirType.XPIR,
            new PbcStdIdxPirConfig.Builder()
                .setPbcStdIdxPirConfig(new XpirStdIdxPirConfig.Builder().build())
                .build()
        });

        configurations.add(new Object[]{
            StdIdxPirFactory.StdIdxPirType.PBC.name() + " " + StdIdxPirType.MUL,
            new PbcStdIdxPirConfig.Builder()
                .setPbcStdIdxPirConfig(new MulStdIdxPirConfig.Builder().build())
                .build()
        });

        configurations.add(new Object[]{
            StdIdxPirFactory.StdIdxPirType.PBC.name() + " " + StdIdxPirType.FAST,
            new PbcStdIdxPirConfig.Builder()
                .setPbcStdIdxPirConfig(new FastStdIdxPirConfig.Builder().build())
                .build()
        });

        configurations.add(new Object[]{
            StdIdxPirFactory.StdIdxPirType.PBC.name() + " " + StdIdxPirType.ONION,
            new PbcStdIdxPirConfig.Builder()
                .setPbcStdIdxPirConfig(new OnionStdIdxPirConfig.Builder().build())
                .build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final PbcStdIdxPirConfig config;
    /**
     * database size
     */
    private final int n;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public PbcPirParamsTest(String name, PbcStdIdxPirConfig config) {
        super(name);
        this.config = config;
        n = 1 << 12;
        secureRandom = new SecureRandom();
    }

    @Test
    public void testPbcPir() {
        testPto(DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelPbcPir() {
        testPto(DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementPbcPir() {
        testPto(LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementPbcPir() {
        testPto(SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testPto(int l, boolean parallel) {
        NaiveDatabase database = NaiveDatabase.createRandom(l, n, secureRandom);
        PbcStdIdxPirServer server = new PbcStdIdxPirServer(firstRpc, secondRpc.ownParty(), config);
        PbcStdIdxPirClient client = new PbcStdIdxPirClient(secondRpc, firstRpc.ownParty(), config);
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

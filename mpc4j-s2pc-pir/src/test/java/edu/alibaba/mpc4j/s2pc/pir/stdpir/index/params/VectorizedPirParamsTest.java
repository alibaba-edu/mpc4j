package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.params;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirClientThread;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirServerThread;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized.VectorizedStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized.VectorizedStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized.VectorizedStdIdxPirParams;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized.VectorizedStdIdxPirServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Vectorized PIR params test.
 *
 * @author Liqiang Peng
 * @date 2024/7/15
 */
@RunWith(Parameterized.class)
public class VectorizedPirParamsTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * default element bit length
     */
    private static final int DEFAULT_ELEMENT_BIT_LENGTH = CommonConstants.STATS_BIT_LENGTH;
    /**
     * large element bit length
     */
    private static final int LARGE_ELEMENT_BIT_LENGTH = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * small element bit length
     */
    private static final int SMALL_ELEMENT_BIT_LENGTH = Byte.SIZE;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            StdIdxPirFactory.StdIdxPirType.VECTOR.name(),
            new VectorizedStdIdxPirConfig.Builder().setParams(new VectorizedStdIdxPirParams(8192, 22)).build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final VectorizedStdIdxPirConfig config;
    /**
     * database size
     */
    private final int n;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public VectorizedPirParamsTest(String name, VectorizedStdIdxPirConfig config) {
        super(name);
        this.config = config;
        n = 1 << 12;
        secureRandom = new SecureRandom();
    }

    @Test
    public void testVectorizedPir() {
        testPto(DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelVectorizedPir() {
        testPto(DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementVectorizedPir() {
        testPto(LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementVectorizedPir() {
        testPto(SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testPto(int l, boolean parallel) {
        NaiveDatabase database = NaiveDatabase.createRandom(l, n, secureRandom);
        VectorizedStdIdxPirServer server = new VectorizedStdIdxPirServer(firstRpc, secondRpc.ownParty(), config);
        VectorizedStdIdxPirClient client = new VectorizedStdIdxPirClient(secondRpc, firstRpc.ownParty(), config);
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

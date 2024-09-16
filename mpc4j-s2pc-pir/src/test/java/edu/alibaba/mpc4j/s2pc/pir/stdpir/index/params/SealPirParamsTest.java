package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.params;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirClientThread;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirServerThread;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j.SealStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j.SealStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j.SealStdIdxPirParams;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j.SealStdIdxPirServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * SEAL PIR params test.
 *
 * @author Liqiang Peng
 * @date 2024/7/15
 */
@RunWith(Parameterized.class)
public class SealPirParamsTest extends AbstractTwoPartyMemoryRpcPto {
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

        // SEAL PIR (1-dimension)
        configurations.add(new Object[]{
                StdIdxPirFactory.StdIdxPirType.SEAL.name() + " (1-dimension)",
                new SealStdIdxPirConfig.Builder().setParams(new SealStdIdxPirParams(4096, 20, 1)).build()
        });
        configurations.add(new Object[]{
                StdIdxPirFactory.StdIdxPirType.SEAL.name() + " (1-dimension)",
                new SealStdIdxPirConfig.Builder().setParams(new SealStdIdxPirParams(8192, 20, 1)).build()
        });
        // SEAL PIR (2-dimension)
        configurations.add(new Object[]{
                StdIdxPirFactory.StdIdxPirType.SEAL.name() + " (2-dimension)",
                new SealStdIdxPirConfig.Builder().setParams(new SealStdIdxPirParams(4096, 20, 2)).build()
        });
        configurations.add(new Object[]{
                StdIdxPirFactory.StdIdxPirType.SEAL.name() + " (2-dimension)",
                new SealStdIdxPirConfig.Builder().setParams(new SealStdIdxPirParams(8192, 20, 2)).build()
        });

        return configurations;
    }

    /**
     * config
     */
    private final SealStdIdxPirConfig config;
    /**
     * database size
     */
    private final int n;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public SealPirParamsTest(String name, SealStdIdxPirConfig config) {
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

    // Failed
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
        SealStdIdxPirServer server = new SealStdIdxPirServer(firstRpc, secondRpc.ownParty(), config);
        SealStdIdxPirClient client = new SealStdIdxPirClient(secondRpc, firstRpc.ownParty(), config);
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
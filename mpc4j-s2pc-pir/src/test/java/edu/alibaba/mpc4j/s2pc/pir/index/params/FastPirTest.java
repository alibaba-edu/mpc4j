package edu.alibaba.mpc4j.s2pc.pir.index.params;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * FastPIR test.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
@RunWith(Parameterized.class)
public class FastPirTest extends AbstractTwoPartyMemoryRpcPto {
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
    /**
     * database size
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        Ayaa21SingleIndexPirConfig fastpirConfig = new Ayaa21SingleIndexPirConfig.Builder().build();
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.FAST_PIR.name(),
            fastpirConfig,
            new Ayaa21SingleIndexPirParams(
                4096, 1073153L, new long[]{1152921504606830593L, 562949953216513L}
            )
        });
        return configurations;
    }

    /**
     * FastPIR config
     */
    private final Ayaa21SingleIndexPirConfig indexPirConfig;
    /**
     * FastPIR params
     */
    private final Ayaa21SingleIndexPirParams indexPirParams;

    public FastPirTest(String name, Ayaa21SingleIndexPirConfig indexPirConfig, Ayaa21SingleIndexPirParams indexPirParams) {
        super(name);
        this.indexPirConfig = indexPirConfig;
        this.indexPirParams = indexPirParams;
    }

    @Test
    public void testFastPir() {
        testFastPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelFastPir() {
        testFastPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementFastPir() {
        testFastPir(indexPirConfig, indexPirParams, LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementFastPir() {
        testFastPir(indexPirConfig, indexPirParams, SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testFastPir(Ayaa21SingleIndexPirConfig config, Ayaa21SingleIndexPirParams indexPirParams,
                            int elementBitLength, boolean parallel) {
        int retrievalIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementBitLength);
        Ayaa21SingleIndexPirServer server = new Ayaa21SingleIndexPirServer(firstRpc, secondRpc.ownParty(), config);
        Ayaa21SingleIndexPirClient client = new Ayaa21SingleIndexPirClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        IndexPirParamsServerThread serverThread = new IndexPirParamsServerThread(server, indexPirParams, database);
        IndexPirParamsClientThread clientThread = new IndexPirParamsClientThread(
            client, indexPirParams, retrievalIndex, SERVER_ELEMENT_SIZE, elementBitLength
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            firstRpc.reset();
            secondRpc.reset();
            // verify result
            ByteBuffer result = clientThread.getRetrievalResult();
            Assert.assertEquals(result, ByteBuffer.wrap(database.getBytesData(retrievalIndex)));
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
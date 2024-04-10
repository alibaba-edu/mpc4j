package edu.alibaba.mpc4j.s2pc.pir.index.params;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.xpir.Mbfk16SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.xpir.Mbfk16SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.xpir.Mbfk16SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.xpir.Mbfk16SingleIndexPirServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * XPIR test.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
@RunWith(Parameterized.class)
public class XPirTest extends AbstractTwoPartyMemoryRpcPto {
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
        Mbfk16SingleIndexPirConfig xpirConfig = new Mbfk16SingleIndexPirConfig.Builder().build();
        // XPIR (1-dimension)
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.XPIR.name() + " (1-dimension)",
            xpirConfig,
            new Mbfk16SingleIndexPirParams(
                4096,
                20,
                1
            )
        });
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.XPIR.name() + " (1-dimension)",
            xpirConfig,
            new Mbfk16SingleIndexPirParams(
                8192,
                20,
                1
            )
        });
        // XPIR (2-dimension)
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.XPIR.name() + " (2-dimension)",
            xpirConfig,
            new Mbfk16SingleIndexPirParams(
                4096,
                20,
                2
            )
        });
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.XPIR.name() + " (2-dimension)",
            xpirConfig,
            new Mbfk16SingleIndexPirParams(
                8192,
                20,
                2
            )
        });
        return configurations;
    }

    /**
     * XPIR config
     */
    private final Mbfk16SingleIndexPirConfig indexPirConfig;
    /**
     * XPIR params
     */
    private final Mbfk16SingleIndexPirParams indexPirParams;

    public XPirTest(String name, Mbfk16SingleIndexPirConfig indexPirConfig, Mbfk16SingleIndexPirParams indexPirParams) {
        super(name);
        this.indexPirConfig = indexPirConfig;
        this.indexPirParams = indexPirParams;
    }

    @Test
    public void testXPir() {
        testXPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelXPir() {
        testXPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementXPir() {
        testXPir(indexPirConfig, indexPirParams, LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementXPir() {
        testXPir(indexPirConfig, indexPirParams, SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testXPir(Mbfk16SingleIndexPirConfig config, Mbfk16SingleIndexPirParams indexPirParams,
                         int elementBitLength, boolean parallel) {
        int retrievalIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementBitLength);
        Mbfk16SingleIndexPirServer server = new Mbfk16SingleIndexPirServer(firstRpc, secondRpc.ownParty(), config);
        Mbfk16SingleIndexPirClient client = new Mbfk16SingleIndexPirClient(secondRpc, firstRpc.ownParty(), config);
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
package edu.alibaba.mpc4j.s2pc.pir.index.params;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.sealpir.Acls18SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.sealpir.Acls18SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.sealpir.Acls18SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.sealpir.Acls18SingleIndexPirServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * SEAL PIR test.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
@RunWith(Parameterized.class)
public class SealPirTest extends AbstractTwoPartyMemoryRpcPto {
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
        Acls18SingleIndexPirConfig sealpirConfig = new Acls18SingleIndexPirConfig.Builder().build();
        // SEAL PIR (1-dimension)
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.SEAL_PIR.name() + " (1-dimension)",
            sealpirConfig,
            new Acls18SingleIndexPirParams(
                4096,
                20,
                1
            )
        });
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.SEAL_PIR.name() + " (1-dimension)",
            sealpirConfig,
            new Acls18SingleIndexPirParams(
                8192,
                20,
                1
            )
        });
        // SEAL PIR (2-dimension)
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.SEAL_PIR.name() + " (2-dimension)",
            sealpirConfig,
            new Acls18SingleIndexPirParams(
                4096,
                20,
                2
            )
        });
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.SEAL_PIR.name() + " (2-dimension)",
            sealpirConfig,
            new Acls18SingleIndexPirParams(
                8192,
                20,
                2
            )
        });
        return configurations;
    }

    /**
     * SEAL PIR config
     */
    private final Acls18SingleIndexPirConfig indexPirConfig;
    /**
     * SEAL PIR params
     */
    private final Acls18SingleIndexPirParams indexPirParams;

    public SealPirTest(String name, Acls18SingleIndexPirConfig indexPirConfig, Acls18SingleIndexPirParams indexPirParams) {
        super(name);
        this.indexPirConfig = indexPirConfig;
        this.indexPirParams = indexPirParams;
    }

    @Test
    public void testSealPir() {
        testSealPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelSealPir() {
        testSealPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementSealPir() {
        testSealPir(indexPirConfig, indexPirParams, LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementSealPir() {
        testSealPir(indexPirConfig, indexPirParams, SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testSealPir(Acls18SingleIndexPirConfig config, Acls18SingleIndexPirParams indexPirParams,
                            int elementBitLength, boolean parallel) {
        int retrievalIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementBitLength);
        Acls18SingleIndexPirServer server = new Acls18SingleIndexPirServer(firstRpc, secondRpc.ownParty(), config);
        Acls18SingleIndexPirClient client = new Acls18SingleIndexPirClient(secondRpc, firstRpc.ownParty(), config);
        // set parallel
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
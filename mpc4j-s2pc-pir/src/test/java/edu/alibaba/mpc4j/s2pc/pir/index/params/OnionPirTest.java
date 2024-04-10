package edu.alibaba.mpc4j.s2pc.pir.index.params;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir.Mcr21SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir.Mcr21SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir.Mcr21SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir.Mcr21SingleIndexPirServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * OnionPIR test.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
@RunWith(Parameterized.class)
public class OnionPirTest extends AbstractTwoPartyMemoryRpcPto {
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
        Mcr21SingleIndexPirConfig onionpirConfig = new Mcr21SingleIndexPirConfig.Builder().build();
        // first dimension is 32
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.ONION_PIR.name() + " (first dimension 32)",
            onionpirConfig,
            new Mcr21SingleIndexPirParams(
                32
            )
        });
        // first dimension is 128
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.ONION_PIR.name() + " (first dimension 128)",
            onionpirConfig,
            new Mcr21SingleIndexPirParams(
                128
            )
        });
        // first dimension is 256
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.ONION_PIR.name() + " (first dimension 256)",
            onionpirConfig,
            new Mcr21SingleIndexPirParams(
                256
            )
        });
        return configurations;
    }

    /**
     * OnionPIR config
     */
    private final Mcr21SingleIndexPirConfig indexPirConfig;
    /**
     * OnionPIR params
     */
    private final Mcr21SingleIndexPirParams indexPirParams;

    public OnionPirTest(String name, Mcr21SingleIndexPirConfig indexPirConfig, Mcr21SingleIndexPirParams indexPirParams) {
        super(name);
        this.indexPirConfig = indexPirConfig;
        this.indexPirParams = indexPirParams;
    }

    @Test
    public void testOnionPir() {
        testOnionPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelOnionPir() {
        testOnionPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementOnionPir() {
        testOnionPir(indexPirConfig, indexPirParams, LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementOnionPir() {
        testOnionPir(indexPirConfig, indexPirParams, SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testOnionPir(Mcr21SingleIndexPirConfig config, Mcr21SingleIndexPirParams indexPirParams,
                             int elementBitLength, boolean parallel) {
        int retrievalIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementBitLength);
        Mcr21SingleIndexPirServer server = new Mcr21SingleIndexPirServer(firstRpc, secondRpc.ownParty(), config);
        Mcr21SingleIndexPirClient client = new Mcr21SingleIndexPirClient(secondRpc, firstRpc.ownParty(), config);
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
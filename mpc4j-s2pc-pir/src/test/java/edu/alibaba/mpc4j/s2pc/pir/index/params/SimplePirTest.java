package edu.alibaba.mpc4j.s2pc.pir.index.params;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Simple PIR test.
 *
 * @author Liqiang Peng
 * @date 2023/5/29
 */
@RunWith(Parameterized.class)
public class SimplePirTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * default element bit length
     */
    private static final int DEFAULT_ELEMENT_BIT_LENGTH = Integer.SIZE;
    /**
     * large element bit length
     */
    private static final int LARGE_ELEMENT_BIT_LENGTH = Double.SIZE;
    /**
     * small element bit length
     */
    private static final int SMALL_ELEMENT_BIT_LENGTH = Byte.SIZE;
    /**
     * database size
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 20;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        Hhcm23SimpleSingleIndexPirConfig config = new Hhcm23SimpleSingleIndexPirConfig.Builder().build();
        // simple PIR
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.SIMPLE_PIR.name(),
            config,
            Hhcm23SimpleSingleIndexPirParams.DEFAULT_PARAMS
        });
        return configurations;
    }

    /**
     * Simple PIR config
     */
    private final Hhcm23SimpleSingleIndexPirConfig indexPirConfig;
    /**
     * Simple PIR params
     */
    private final Hhcm23SimpleSingleIndexPirParams indexPirParams;

    public SimplePirTest(String name, Hhcm23SimpleSingleIndexPirConfig indexPirConfig,
                         Hhcm23SimpleSingleIndexPirParams indexPirParams) {
        super(name);
        this.indexPirConfig = indexPirConfig;
        this.indexPirParams = indexPirParams;
    }

    @Test
    public void testSimplePir() {
        testSimplePir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelSimplePir() {
        testSimplePir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementSimplePir() {
        testSimplePir(indexPirConfig, indexPirParams, LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementSimplePir() {
        testSimplePir(indexPirConfig, indexPirParams, SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testSimplePir(Hhcm23SimpleSingleIndexPirConfig config, Hhcm23SimpleSingleIndexPirParams indexPirParams,
                              int elementBitLength, boolean parallel) {
        int retrievalIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementBitLength);
        Hhcm23SimpleSingleIndexPirServer server = new Hhcm23SimpleSingleIndexPirServer(firstRpc, secondRpc.ownParty(), config);
        Hhcm23SimpleSingleIndexPirClient client = new Hhcm23SimpleSingleIndexPirClient(secondRpc, firstRpc.ownParty(), config);
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

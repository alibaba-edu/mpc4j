package edu.alibaba.mpc4j.s2pc.pir.index.params;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir.Mr23SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir.Mr23SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir.Mr23SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir.Mr23SingleIndexPirServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Vectorized PIR test.
 *
 * @author Liqiang Peng
 * @date 2023/3/24
 */
@RunWith(Parameterized.class)
public class VectorizedPirTest extends AbstractTwoPartyMemoryRpcPto {
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
    private static final int SMALL_ELEMENT_BIT_LENGTH = 2;
    /**
     * database size
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 16;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        Mr23SingleIndexPirConfig pirConfig = new Mr23SingleIndexPirConfig.Builder().build();
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.VECTORIZED_PIR.name(),
            pirConfig,
            new Mr23SingleIndexPirParams(8192, 22)
        });
        return configurations;
    }

    /**
     * Vectorized PIR config
     */
    private final Mr23SingleIndexPirConfig indexPirConfig;
    /**
     * Vectorized PIR params
     */
    private final Mr23SingleIndexPirParams indexPirParams;

    public VectorizedPirTest(String name, Mr23SingleIndexPirConfig indexPirConfig, Mr23SingleIndexPirParams indexPirParams) {
        super(name);
        this.indexPirConfig = indexPirConfig;
        this.indexPirParams = indexPirParams;
    }

    @Test
    public void testVectorizedPir() {
        testVectorizedPir(indexPirConfig, indexPirParams, SERVER_ELEMENT_SIZE, DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelVectorizedPir() {
        testVectorizedPir(indexPirConfig, indexPirParams, SERVER_ELEMENT_SIZE, DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementVectorizedPir() {
        testVectorizedPir(indexPirConfig, indexPirParams, SERVER_ELEMENT_SIZE, LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testOneElementVectorizedPir() {
        testVectorizedPir(indexPirConfig, indexPirParams, 1, DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSpecialElementSizeVectorizedPir() {
        testVectorizedPir(indexPirConfig, indexPirParams, SERVER_ELEMENT_SIZE + 1, DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementVectorizedPir() {
        testVectorizedPir(indexPirConfig, indexPirParams, SERVER_ELEMENT_SIZE, SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testVectorizedPir(Mr23SingleIndexPirConfig config, Mr23SingleIndexPirParams indexPirParams,
                                  int serverElementSize, int elementBitLength, boolean parallel) {
        int retrievalIndex = PirUtils.generateRetrievalIndex(serverElementSize);
        NaiveDatabase database = PirUtils.generateDataBase(serverElementSize, elementBitLength);
        Mr23SingleIndexPirServer server = new Mr23SingleIndexPirServer(firstRpc, secondRpc.ownParty(), config);
        Mr23SingleIndexPirClient client = new Mr23SingleIndexPirClient(secondRpc, firstRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        IndexPirParamsServerThread serverThread = new IndexPirParamsServerThread(server, indexPirParams, database);
        IndexPirParamsClientThread clientThread = new IndexPirParamsClientThread(
            client, indexPirParams, retrievalIndex, serverElementSize, elementBitLength
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
            System.out.println(indexPirParams.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
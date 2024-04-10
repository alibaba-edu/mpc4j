package edu.alibaba.mpc4j.s2pc.pir.batchpir;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.labelpsi.Cmg21BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir.Mr23BatchIndexPirConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * batch PIR test.
 *
 * @author Liqiang Peng
 * @date 2023/3/9
 */
@RunWith(Parameterized.class)
public class BatchPirTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * default bit length
     */
    private static final int DEFAULT_BIT_LENGTH = Double.SIZE;
    /**
     * small bit length
     */
    private static final int SMALL_BIT_LENGTH = Integer.SIZE;
    /**
     * large bit length
     */
    private static final int LARGE_BIT_LENGTH = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * small server element size
     */
    private static final int SMALL_SERVER_ELEMENT_SIZE = 1 << 10;
    /**
     * default server element size
     */
    private static final int DEFAULT_SERVER_ELEMENT_SIZE = 1 << 14;
    /**
     * default retrieval size
     */
    private static final int DEFAULT_RETRIEVAL_SIZE = 1 << 6;
    /**
     * special retrieval size
     */
    private static final int SPECIAL_RETRIEVAL_SIZE = (1 << 2) + 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // vectorized batch PIR
        configurations.add(new Object[]{
            BatchIndexPirFactory.BatchIndexPirType.VECTORIZED_BATCH_PIR.name(),
            new Mr23BatchIndexPirConfig.Builder().build()
        });
        // CMG21 batch PIR
        configurations.add(new Object[]{
            BatchIndexPirFactory.BatchIndexPirType.LABEL_PSI.name(),
            new Cmg21BatchIndexPirConfig.Builder().build()
        });
        return configurations;
    }

    /**
     * batch PIR config
     */
    private final BatchIndexPirConfig config;

    public BatchPirTest(String name, BatchIndexPirConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testSmallBitLength() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, DEFAULT_RETRIEVAL_SIZE, SMALL_BIT_LENGTH, true);
    }

    @Test
    public void testDefaultBitLengthParallel() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, DEFAULT_RETRIEVAL_SIZE, DEFAULT_BIT_LENGTH, true);
    }

    @Test
    public void testDefaultBitLength() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, DEFAULT_RETRIEVAL_SIZE, DEFAULT_BIT_LENGTH, false);
    }

    @Test
    public void testLargeBitLength() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, DEFAULT_RETRIEVAL_SIZE, LARGE_BIT_LENGTH, true);
    }

    @Test
    public void test2Retrieval() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, 2, DEFAULT_BIT_LENGTH, true);
    }

    @Test
    public void test1Retrieval() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, 1, DEFAULT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementSize() {
        testPto(SMALL_SERVER_ELEMENT_SIZE, DEFAULT_RETRIEVAL_SIZE, DEFAULT_BIT_LENGTH, true);
    }

    @Test
    public void testSpecialElementSize() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE + 1, DEFAULT_RETRIEVAL_SIZE, DEFAULT_BIT_LENGTH, true);
    }


    @Test
    public void test1() {
        testPto(1 << 20, 1 << 8, 1, true);
    }

    @Test
    public void testSpecialRetrievalSize() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, SPECIAL_RETRIEVAL_SIZE, DEFAULT_BIT_LENGTH, true);
    }

    public void testPto(int serverElementSize, int retrievalIndexSize, int elementBitLength, boolean parallel) {
        Set<Integer> retrievalIndexSet = PirUtils.generateRetrievalIndexSet(serverElementSize, retrievalIndexSize);
        NaiveDatabase database = PirUtils.generateDataBase(serverElementSize, elementBitLength);
        // create instance
        BatchIndexPirServer server = BatchIndexPirFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        BatchIndexPirClient client = BatchIndexPirFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        BatchPirServerThread serverThread = new BatchPirServerThread(server, database, retrievalIndexSize);
        BatchPirClientThread clientThread = new BatchPirClientThread(
            client, new ArrayList<>(retrievalIndexSet), elementBitLength, serverElementSize, retrievalIndexSize
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            // verify
            Map<Integer, byte[]> result = clientThread.getRetrievalResult();
            Assert.assertEquals(retrievalIndexSize, result.size());
            result.forEach(
                (key, value) -> Assert.assertEquals(ByteBuffer.wrap(database.getBytesData(key)), ByteBuffer.wrap(value))
            );
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
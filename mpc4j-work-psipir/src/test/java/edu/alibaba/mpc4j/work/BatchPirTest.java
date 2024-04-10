package edu.alibaba.mpc4j.work;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.work.psipir.Lpzl24BatchPirConfig;
import edu.alibaba.mpc4j.work.vectoried.VectorizedBatchPirConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.stream.IntStream;

/**
 * batch PIR test.
 *
 * @author Liqiang Peng
 * @date 2023/3/9
 */
@RunWith(Parameterized.class)
public class BatchPirTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * small server element size
     */
    private static final int SMALL_SERVER_ELEMENT_SIZE = 1 << 10;
    /**
     * default server element size
     */
    private static final int DEFAULT_SERVER_ELEMENT_SIZE = 1 << 14;
    /**
     * large server element size
     */
    private static final int LARGE_SERVER_ELEMENT_SIZE = 1 << 18;
    /**
     * default retrieval size
     */
    private static final int DEFAULT_RETRIEVAL_SIZE = 1 << 6;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // vectorized batch PIR
        configurations.add(new Object[]{
            BatchPirFactory.BatchIndexPirType.VECTORIZED_BATCH_PIR.name(),
            new VectorizedBatchPirConfig.Builder().build()
        });
        // PSI PIR
        configurations.add(new Object[]{
            BatchPirFactory.BatchIndexPirType.PSI_PIR.name(),
            new Lpzl24BatchPirConfig.Builder().build()
        });
        return configurations;
    }

    /**
     * batch PIR config
     */
    private final BatchPirConfig config;

    public BatchPirTest(String name, BatchPirConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testDefaultElementSizeParallel() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, DEFAULT_RETRIEVAL_SIZE, true);
    }

    @Test
    public void testDefaultElementSize() {
        testPto(DEFAULT_SERVER_ELEMENT_SIZE, DEFAULT_RETRIEVAL_SIZE, false);
    }

    @Test
    public void testLargeElementSize() {
        testPto(LARGE_SERVER_ELEMENT_SIZE, DEFAULT_RETRIEVAL_SIZE, true);
    }

    @Test
    public void testSmallElementSize() {
        testPto(SMALL_SERVER_ELEMENT_SIZE, DEFAULT_RETRIEVAL_SIZE, true);
    }

    public void testPto(int serverElementSize, int retrievalIndexSize, boolean parallel) {
        Set<Integer> retrievalIndexSet = generateRetrievalIndexSet(serverElementSize, retrievalIndexSize);
        BitVector database = generateBitVector(serverElementSize);
        // create instance
        BatchPirServer server = BatchPirFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        BatchPirClient client = BatchPirFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        BatchPirServerThread serverThread = new BatchPirServerThread(server, database, retrievalIndexSize);
        BatchPirClientThread clientThread = new BatchPirClientThread(
            client, new ArrayList<>(retrievalIndexSet), serverElementSize, retrievalIndexSize
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            // verify
            Map<Integer, Boolean> result = clientThread.getRetrievalResult();
            Assert.assertEquals(retrievalIndexSize, result.size());
            result.forEach((key, value) -> Assert.assertEquals(database.get(key), value));
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * generate random retrieval index set.
     *
     * @param elementSize   element size.
     * @param retrievalSize retrieval size.
     * @return random retrieval index set.
     */
    public Set<Integer> generateRetrievalIndexSet(int elementSize, int retrievalSize) {
        Set<Integer> indexSet = new HashSet<>();
        while (indexSet.size() < retrievalSize) {
            int index = SECURE_RANDOM.nextInt(elementSize);
            indexSet.add(index);
        }
        return indexSet;
    }

    /**
     * generate random bit vector.
     *
     * @param num bit vector length.
     * @return random bit vector.
     */
    public BitVector generateBitVector(int num) {
        BitVector bitVector = BitVectorFactory.createZeros(num);
        IntStream.range(0, num)
            .filter(i -> SECURE_RANDOM.nextBoolean())
            .forEach(i -> bitVector.set(i, true));
        return bitVector;
    }
}
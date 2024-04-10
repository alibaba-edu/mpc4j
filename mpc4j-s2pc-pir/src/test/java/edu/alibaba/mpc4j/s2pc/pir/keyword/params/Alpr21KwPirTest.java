package edu.alibaba.mpc4j.s2pc.pir.keyword.params;

import com.google.common.collect.Lists;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.naive.NaiveBatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.simplepir.CuckooHashBatchSimplePirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21.Alpr21KwPirClient;
import edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21.Alpr21KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21.Alpr21KwPirParams;
import edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21.Alpr21KwPirServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * ALPR21 keyword PIR test.
 *
 * @author Liqiang Peng
 * @date 2023/7/14
 */
@RunWith(Parameterized.class)
public class Alpr21KwPirTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * repeat time
     */
    private static final int REPEAT_TIME = 1;
    /**
     * default label byte length
     */
    private static final int DEFAULT_LABEL_BYTE_LENGTH = Double.BYTES;
    /**
     * server element size
     */
    private static final int SERVER_MAP_SIZE = 1 << 18;
    /**
     * large retrieval size
     */
    private static final int LARGE_RETRIEVAL_SIZE = 1 << 8;
    /**
     * default retrieval size
     */
    private static final int DEFAULT_RETRIEVAL_SIZE = 1 << 6;
    /**
     * small retrieval size
     */
    private static final int SMALL_RETRIEVAL_SIZE = 1 << 4;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // ALPR21
        configurations.add(new Object[]{
            KwPirFactory.KwPirType.ALPR21.name() + " truncation size 3 bytes - communication optimal",
            new Alpr21KwPirConfig.Builder().build(),
            new Alpr21KwPirParams(CommonConstants.BLOCK_BYTE_LENGTH, 3)
        });
        configurations.add(new Object[]{
            KwPirFactory.KwPirType.ALPR21.name() + " truncation size 3 bytes - computation optimal",
            new Alpr21KwPirConfig.Builder()
                .setBatchIndexPirConfig(
                    new CuckooHashBatchSimplePirConfig.Builder()
                        .setCommunicationOptimal(false)
                        .build()
                ).build(),
            new Alpr21KwPirParams(CommonConstants.BLOCK_BYTE_LENGTH, 3)
        });
        configurations.add(new Object[]{
            KwPirFactory.KwPirType.ALPR21.name() + " truncation size 3 bytes - naive batch simple PIR",
            new Alpr21KwPirConfig.Builder()
                .setBatchIndexPirConfig(new NaiveBatchIndexPirConfig.Builder().build())
                .build(),
            new Alpr21KwPirParams(CommonConstants.BLOCK_BYTE_LENGTH, 3)
        });
        return configurations;
    }

    /**
     * ALPR21 keyword PIR config
     */
    private final Alpr21KwPirConfig config;
    /**
     * ALPR21 keyword PIR params
     */
    private final Alpr21KwPirParams params;

    public Alpr21KwPirTest(String name, Alpr21KwPirConfig config, Alpr21KwPirParams params) {
        super(name);
        this.config = config;
        this.params = params;
    }

    @Test
    public void testSmallRetrievalSizeParallel() {
        testPir(params, SMALL_RETRIEVAL_SIZE, config, true);
    }

    @Test
    public void testDefaultRetrievalSizeParallel() {
        testPir(params, DEFAULT_RETRIEVAL_SIZE, config, true);
    }

    @Test
    public void testLargeRetrievalSizeParallel() {
        testPir(params, LARGE_RETRIEVAL_SIZE, config, true);
    }

    public void testPir(Alpr21KwPirParams kwPirParams, int retrievalSize, Alpr21KwPirConfig config, boolean parallel) {
        List<Set<ByteBuffer>> randomSets = PirUtils.generateByteBufferSets(SERVER_MAP_SIZE, retrievalSize, REPEAT_TIME);
        Map<ByteBuffer, byte[]> keywordLabelMap = PirUtils.generateKeywordByteBufferLabelMap(
            randomSets.get(0), DEFAULT_LABEL_BYTE_LENGTH
        );
        // create instances
        Alpr21KwPirServer server = new Alpr21KwPirServer(firstRpc, secondRpc.ownParty(), config);
        Alpr21KwPirClient client = new Alpr21KwPirClient(secondRpc, firstRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        KwPirParamsServerThread serverThread = new KwPirParamsServerThread(
            server, kwPirParams, keywordLabelMap, retrievalSize, DEFAULT_LABEL_BYTE_LENGTH, REPEAT_TIME
        );
        KwPirParamsClientThread clientThread = new KwPirParamsClientThread(
            client,
            kwPirParams,
            Lists.newArrayList(randomSets.subList(1, REPEAT_TIME + 1)),
            retrievalSize,
            SERVER_MAP_SIZE,
            DEFAULT_LABEL_BYTE_LENGTH
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            // verify result
            for (int index = 0; index < REPEAT_TIME; index++) {
                Set<ByteBuffer> intersectionSet = new HashSet<>(randomSets.get(index + 1));
                intersectionSet.retainAll(randomSets.get(0));
                Map<ByteBuffer, byte[]> pirResult = clientThread.getRetrievalResult(index);
                Assert.assertEquals(intersectionSet.size(), pirResult.size());
                pirResult.forEach((key, value) -> Assert.assertArrayEquals(value, keywordLabelMap.get(key)));
            }
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

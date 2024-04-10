package edu.alibaba.mpc4j.s2pc.pir.keyword.params;

import com.google.common.collect.Lists;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.keyword.*;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * CMG21 keyword PIR test.
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
@RunWith(Parameterized.class)
public class Cmg21KwPirTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * repeat time
     */
    private static final int REPEAT_TIME = 1;
    /**
     * short label byte length
     */
    private static final int SHORT_LABEL_BYTE_LENGTH = CommonConstants.STATS_BYTE_LENGTH;
    /**
     * default label byte length
     */
    private static final int DEFAULT_LABEL_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * long label byte length
     */
    private static final int LONG_LABEL_BYTE_LENGTH = CommonConstants.STATS_BIT_LENGTH;
    /**
     * server element size
     */
    private static final int SERVER_MAP_SIZE = 100000;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CMG21
        configurations.add(new Object[]{
            KwPirFactory.KwPirType.CMG21.name() + " max client set size 1", new Cmg21KwPirConfig.Builder().build(),
            Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_1
        });
        configurations.add(new Object[]{
            KwPirFactory.KwPirType.CMG21.name() + " max client set size 4096", new Cmg21KwPirConfig.Builder().build(),
            Cmg21KwPirParams.SERVER_1M_CLIENT_MAX_4096
        });

        return configurations;
    }

    /**
     * CMG21 keyword PIR config
     */
    private final Cmg21KwPirConfig config;
    /**
     * CMG21 keyword PIR params
     */
    private final Cmg21KwPirParams params;

    public Cmg21KwPirTest(String name, Cmg21KwPirConfig config, Cmg21KwPirParams params) {
        super(name);
        this.config = config;
        this.params = params;
    }

    @Test
    public void testShortLabelParallel() {
        testPir(params, SHORT_LABEL_BYTE_LENGTH, config, true);
    }

    @Test
    public void testDefaultLabelParallel() {
        testPir(params, DEFAULT_LABEL_BYTE_LENGTH, config, true);
    }

    @Test
    public void testLongLabelParallel() {
        testPir(params, LONG_LABEL_BYTE_LENGTH, config, true);
    }

    public void testPir(Cmg21KwPirParams kwPirParams, int labelByteLength, Cmg21KwPirConfig config, boolean parallel) {
        int retrievalSize = kwPirParams.maxRetrievalSize();
        List<Set<ByteBuffer>> randomSets = PirUtils.generateByteBufferSets(SERVER_MAP_SIZE, retrievalSize, REPEAT_TIME);
        Map<ByteBuffer, byte[]> keywordLabelMap = PirUtils.generateKeywordByteBufferLabelMap(randomSets.get(0), labelByteLength);
        // create instances
        Cmg21KwPirServer server = new Cmg21KwPirServer(firstRpc, secondRpc.ownParty(), config);
        Cmg21KwPirClient client = new Cmg21KwPirClient(secondRpc, firstRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        KwPirParamsServerThread serverThread = new KwPirParamsServerThread(
            server, kwPirParams, keywordLabelMap, retrievalSize, labelByteLength, REPEAT_TIME
        );
        KwPirParamsClientThread clientThread = new KwPirParamsClientThread(
            client,
            kwPirParams,
            Lists.newArrayList(randomSets.subList(1, REPEAT_TIME + 1)),
            retrievalSize,
            SERVER_MAP_SIZE,
            labelByteLength
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



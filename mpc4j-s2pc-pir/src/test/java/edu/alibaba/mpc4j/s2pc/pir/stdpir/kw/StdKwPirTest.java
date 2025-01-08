package edu.alibaba.mpc4j.s2pc.pir.stdpir.kw;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.fast.FastStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal.SealStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.alpr21.Alpr21StdKwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.pantheon.PantheonStdKwPirConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * standard keyword PIR test.
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
@RunWith(Parameterized.class)
public class StdKwPirTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * short label byte length
     */
    private static final int SHORT_LABEL_BYTE_LENGTH = Double.BYTES;
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
    private static final int SERVER_MAP_SIZE = 1 << 8;
    /**
     * client element size
     */
    private static final int CLIENT_SET_SIZE = 2;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // Pantheon
        configurations.add(new Object[]{
            StdKwPirFactory.StdKwPirType.Pantheon.name(), new PantheonStdKwPirConfig.Builder().build()
        });
        // ALPR21 + SEAL PIR
        configurations.add(new Object[]{
            StdKwPirFactory.StdKwPirType.ALPR21.name() + " SEAL PIR",
            new Alpr21StdKwPirConfig.Builder()
                .setPbcableStdIdxPirConfig(new SealStdIdxPirConfig.Builder().build())
                .build()
        });
        // ALPR21 + Fast PIR
        configurations.add(new Object[]{
            StdKwPirFactory.StdKwPirType.ALPR21.name() + " Fast PIR",
            new Alpr21StdKwPirConfig.Builder()
                .setPbcableStdIdxPirConfig(new FastStdIdxPirConfig.Builder().build())
                .build()
        });

        return configurations;
    }

    /**
     * keyword PIR config
     */
    private final StdKwPirConfig config;

    public StdKwPirTest(String name, StdKwPirConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testShortLabelParallel() {
        testPir(SHORT_LABEL_BYTE_LENGTH, config, true);
    }

    @Test
    public void testDefaultLabelParallel() {
        testPir(DEFAULT_LABEL_BYTE_LENGTH, config, true);
    }

    @Test
    public void testLongLabelParallel() {
        testPir(LONG_LABEL_BYTE_LENGTH, config, true);
    }

    public void testPir(int byteL, StdKwPirConfig config, boolean parallel) {
        List<Set<ByteBuffer>> randomSets = PirUtils.generateByteBufferSets(SERVER_MAP_SIZE, CLIENT_SET_SIZE, 1);
        Map<ByteBuffer, byte[]> keyValueMap = PirUtils.generateKeywordByteBufferLabelMap(randomSets.get(0), byteL);
        // create instances
        StdKwPirServer<ByteBuffer> server = StdKwPirFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        StdKwPirClient<ByteBuffer> client = StdKwPirFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        StdKwPirServerThread serverThread = new StdKwPirServerThread(
            server, keyValueMap, byteL * Byte.SIZE, CLIENT_SET_SIZE
        );
        ArrayList<ByteBuffer> keys = new ArrayList<>(randomSets.get(1));
        StdKwPirClientThread clientThread = new StdKwPirClientThread(
            client, keys, SERVER_MAP_SIZE, byteL * Byte.SIZE
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            // verify result
            byte[][] actualResult = clientThread.getRetrievalResult();
            for (int i = 0; i < keys.size(); i++) {
                if (keyValueMap.containsKey(keys.get(i))) {
                    Assert.assertArrayEquals(keyValueMap.get(keys.get(i)), actualResult[i]);
                }
            }
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
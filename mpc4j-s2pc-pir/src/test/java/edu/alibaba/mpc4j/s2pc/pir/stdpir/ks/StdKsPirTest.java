package edu.alibaba.mpc4j.s2pc.pir.stdpir.ks;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi.LabelpsiStdKsPirConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * standard KSPIR test.
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
@RunWith(Parameterized.class)
public class StdKsPirTest extends AbstractTwoPartyMemoryRpcPto {
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

        // LABEL PSI
        configurations.add(new Object[]{
            StdKsPirFactory.StdKsPirType.Label_PSI.name(), new LabelpsiStdKsPirConfig.Builder().build()
        });

        return configurations;
    }

    /**
     * keyword PIR config
     */
    private final StdKsPirConfig config;

    public StdKsPirTest(String name, StdKsPirConfig config) {
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

    public void testPir(int byteL, StdKsPirConfig config, boolean parallel) {
        List<Set<ByteBuffer>> randomSets = PirUtils.generateByteBufferSets(SERVER_MAP_SIZE, CLIENT_SET_SIZE, 1);
        Map<ByteBuffer, byte[]> keyValueMap = PirUtils.generateKeywordByteBufferLabelMap(
            randomSets.getFirst(), byteL
        );
        // create instances
        StdKsPirServer<ByteBuffer> server = StdKsPirFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        StdKsPirClient<ByteBuffer> client = StdKsPirFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        StdKsPirServerThread serverThread = new StdKsPirServerThread(server, keyValueMap, byteL * Byte.SIZE, CLIENT_SET_SIZE);
        ArrayList<ByteBuffer> keys = new ArrayList<>(randomSets.getLast());
        StdKsPirClientThread clientThread = new StdKsPirClientThread(
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
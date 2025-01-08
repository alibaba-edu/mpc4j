package edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.params;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.StdKsPirClientThread;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.StdKsPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.StdKsPirServerThread;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi.LabelpsiStdKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi.LabelpsiStdKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi.LabelpsiStdKsPirParams;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi.LabelpsiStdKsPirServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * label PSI KS PIR test.
 *
 * @author Liqiang Peng
 * @date 2022/6/22
 */
@RunWith(Parameterized.class)
public class LabelpsiKsPirTest extends AbstractTwoPartyMemoryRpcPto {
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
    private static final int SERVER_MAP_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CMG21
        configurations.add(new Object[]{
            StdKsPirFactory.StdKsPirType.Label_PSI.name() + " max client set size 1",
            new LabelpsiStdKsPirConfig.Builder().setParams(LabelpsiStdKsPirParams.SERVER_1M_CLIENT_MAX_1).build()
        });
        configurations.add(new Object[]{
            StdKsPirFactory.StdKsPirType.Label_PSI.name() + " max client set size 4096",
            new LabelpsiStdKsPirConfig.Builder().setParams(LabelpsiStdKsPirParams.SERVER_1M_CLIENT_MAX_4096).build()
        });

        return configurations;
    }

    /**
     * label PSI keyword PIR config
     */
    private final LabelpsiStdKsPirConfig config;
    /**
     * label PSI keyword PIR params
     */
    private final LabelpsiStdKsPirParams params;

    public LabelpsiKsPirTest(String name, LabelpsiStdKsPirConfig config) {
        super(name);
        this.config = config;
        this.params = config.getParams();
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

    public void testPir(LabelpsiStdKsPirParams params, int byteL, LabelpsiStdKsPirConfig config, boolean parallel) {
        int retrievalSize = params.maxRetrievalSize();
        List<Set<ByteBuffer>> randomSets = PirUtils.generateByteBufferSets(SERVER_MAP_SIZE, retrievalSize, 1);
        Map<ByteBuffer, byte[]> keyValueMap = PirUtils.generateKeywordByteBufferLabelMap(randomSets.get(0), byteL);
        // create instances
        LabelpsiStdKsPirServer<ByteBuffer> server = new LabelpsiStdKsPirServer<>(firstRpc, secondRpc.ownParty(), config);
        LabelpsiStdKsPirClient<ByteBuffer> client = new LabelpsiStdKsPirClient<>(secondRpc, firstRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        StdKsPirServerThread serverThread = new StdKsPirServerThread(server, keyValueMap, byteL * Byte.SIZE, retrievalSize);
        ArrayList<ByteBuffer> keys = new ArrayList<>(randomSets.get(1));
        StdKsPirClientThread clientThread = new StdKsPirClientThread(client, keys, SERVER_MAP_SIZE, byteL * Byte.SIZE);
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



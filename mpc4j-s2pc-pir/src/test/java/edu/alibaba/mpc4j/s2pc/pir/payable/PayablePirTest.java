package edu.alibaba.mpc4j.s2pc.pir.payable;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.payable.zlp23.Zlp23PayablePirConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * payable PIR test.
 *
 * @author Liqiang Peng
 * @date 2023/9/7
 */
@RunWith(Parameterized.class)
public class PayablePirTest extends AbstractTwoPartyMemoryRpcPto {
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
    private static final int SERVER_MAP_SIZE = 1 << 20;
    /**
     * client element size
     */
    private static final int CLIENT_SET_SIZE = 1;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // ZLP23
        configurations.add(new Object[]{
            PayablePirFactory.PayablePirType.ZLP23.name(), new Zlp23PayablePirConfig.Builder().build()
        });
        return configurations;
    }

    /**
     * payable PIR config
     */
    private final PayablePirConfig config;

    public PayablePirTest(String name, PayablePirConfig config) {
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

    public void testPir(int labelByteLength, PayablePirConfig config, boolean parallel) {
        List<Set<ByteBuffer>> randomSets = PirUtils.generateByteBufferSets(SERVER_MAP_SIZE, CLIENT_SET_SIZE, 1);
        ByteBuffer retrievalElement = new ArrayList<>(randomSets.get(1)).get(0);
        Map<ByteBuffer, byte[]> keywordLabelMap = PirUtils.generateKeywordByteBufferLabelMap(
            randomSets.get(0), labelByteLength
        );
        // create instances
        PayablePirServer server = PayablePirFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        PayablePirClient client = PayablePirFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        PayablePirServerThread serverThread = new PayablePirServerThread(server, keywordLabelMap, labelByteLength);
        PayablePirClientThread clientThread = new PayablePirClientThread(
            client, retrievalElement, SERVER_MAP_SIZE, labelByteLength
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            // verify result
            Set<ByteBuffer> intersectionSet = new HashSet<>(randomSets.get(1));
            intersectionSet.retainAll(randomSets.get(0));
            boolean serverOutput = serverThread.getServerOutput();
            byte[] clientOutput = clientThread.getClientOutput();
            if (intersectionSet.size() == 0) {
                Assert.assertFalse(serverOutput);
                Assert.assertNull(clientOutput);
            } else {
                Assert.assertTrue(serverOutput);
                Assert.assertArrayEquals(clientOutput, keywordLabelMap.get(retrievalElement));
            }
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
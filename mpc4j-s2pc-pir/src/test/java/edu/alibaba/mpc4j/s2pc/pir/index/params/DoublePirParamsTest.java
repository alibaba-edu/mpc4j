package edu.alibaba.mpc4j.s2pc.pir.index.params;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Double PIR test.
 *
 * @author Liqiang Peng
 * @date 2023/5/29
 */
@RunWith(Parameterized.class)
public class DoublePirParamsTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * default element bit length
     */
    private static final int DEFAULT_ELEMENT_BIT_LENGTH = Double.SIZE;
    /**
     * large element bit length
     */
    private static final int LARGE_ELEMENT_BIT_LENGTH = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * small element bit length
     */
    private static final int SMALL_ELEMENT_BIT_LENGTH = Byte.SIZE;
    /**
     * database size
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        Hhcm23DoubleSingleIndexPirConfig config = new Hhcm23DoubleSingleIndexPirConfig.Builder().build();
        // double PIR
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.DOUBLE_PIR.name(),
            config,
            Hhcm23DoubleSingleIndexPirParams.DEFAULT_PARAMS
        });
        return configurations;
    }

    /**
     * Double PIR config
     */
    private final Hhcm23DoubleSingleIndexPirConfig config;
    /**
     * Double PIR params
     */
    private final Hhcm23DoubleSingleIndexPirParams params;

    public DoublePirParamsTest(String name, Hhcm23DoubleSingleIndexPirConfig config, Hhcm23DoubleSingleIndexPirParams params) {
        super(name);
        this.config = config;
        this.params = params;
    }

    @Test
    public void testDoublePir() {
        testDoublePir(config, params, DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelDoublePir() {
        testDoublePir(config, params, DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementDoublePir() {
        testDoublePir(config, params, LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementDoublePir() {
        testDoublePir(config, params, SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testDoublePir(Hhcm23DoubleSingleIndexPirConfig config, Hhcm23DoubleSingleIndexPirParams params,
                              int elementBitLength, boolean parallel) {
        int retrievalIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementBitLength);
        Hhcm23DoubleSingleIndexPirServer server = new Hhcm23DoubleSingleIndexPirServer(firstRpc, secondRpc.ownParty(), config);
        Hhcm23DoubleSingleIndexPirClient client = new Hhcm23DoubleSingleIndexPirClient(secondRpc, firstRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        IndexPirParamsServerThread serverThread = new IndexPirParamsServerThread(server, params, database);
        IndexPirParamsClientThread clientThread = new IndexPirParamsClientThread(
            client, params, retrievalIndex, SERVER_ELEMENT_SIZE, elementBitLength
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            firstRpc.reset();
            secondRpc.reset();
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

package edu.alibaba.mpc4j.s2pc.pir.index.params;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Constant-Weight PIR test
 *
 * @author Qixian Zhou
 * @date 2023/5/29
 */
@RunWith(Parameterized.class)
public class ConstantWeightPirParamsTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * default element bit length
     */
    private static final int DEFAULT_ELEMENT_BIT_LENGTH = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * large element bit length
     */
    private static final int LARGE_ELEMENT_BIT_LENGTH = 10000;
    /**
     * small element bit length
     */
    private static final int SMALL_ELEMENT_BIT_LENGTH = CommonConstants.STATS_BIT_LENGTH;
    /**
     * database size
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        Mk22SingleIndexPirConfig config = new Mk22SingleIndexPirConfig.Builder().build();
        // ConstantWeight PIR (CONSTANT_WEIGHT_EQ)
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.CONSTANT_WEIGHT_PIR.name() + " (CONSTANT_WEIGHT_EQ)",
            config, new Mk22SingleIndexPirParams(2, 16384, 21, Mk22SingleIndexPirParams.EqualityType.CONSTANT_WEIGHT)
        });
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.CONSTANT_WEIGHT_PIR.name() + " (FOLKLORE)",
            config, new Mk22SingleIndexPirParams(2, 16384, 21, Mk22SingleIndexPirParams.EqualityType.FOLKLORE)
        });

        return configurations;
    }

    /**
     * ConstantWeight PIR config
     */
    private final Mk22SingleIndexPirConfig indexPirConfig;
    /**
     * ConstantWeight PIR params
     */
    private final Mk22SingleIndexPirParams indexPirParams;

    public ConstantWeightPirParamsTest(String name, Mk22SingleIndexPirConfig indexPirConfig,
                                       Mk22SingleIndexPirParams indexPirParams) {
        super(name);
        this.indexPirConfig = indexPirConfig;
        this.indexPirParams = indexPirParams;
    }

    @Test
    public void testConstantWeightPir() {
        testConstantWeightPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelConstantWeightPir() {
        testConstantWeightPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementMulPir() {
        testConstantWeightPir(indexPirConfig, indexPirParams, LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementMulPir() {
        testConstantWeightPir(indexPirConfig, indexPirParams, SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testConstantWeightPir(Mk22SingleIndexPirConfig config, Mk22SingleIndexPirParams indexPirParams,
                                      int elementBitLength, boolean parallel) {
        int retrievalSingleIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementBitLength);
        Mk22SingleIndexPirServer server = new Mk22SingleIndexPirServer(firstRpc, secondRpc.ownParty(), config);
        Mk22SingleIndexPirClient client = new Mk22SingleIndexPirClient(secondRpc, firstRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        IndexPirParamsServerThread serverThread = new IndexPirParamsServerThread(server, indexPirParams, database);
        IndexPirParamsClientThread clientThread = new IndexPirParamsClientThread(
            client, indexPirParams, retrievalSingleIndex, SERVER_ELEMENT_SIZE, elementBitLength
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            // verify result
            ByteBuffer result = clientThread.getRetrievalResult();
            Assert.assertEquals(result, ByteBuffer.wrap(database.getBytesData(retrievalSingleIndex)));
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

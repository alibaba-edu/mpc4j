package edu.alibaba.mpc4j.s2pc.pir.index.params;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirParams;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Mul PIR test.
 *
 * @author Qixian Zhou
 * @date 2023/5/29
 */
@RunWith(Parameterized.class)
public class MulPirTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * default element bit length
     */
    private static final int DEFAULT_ELEMENT_BIT_LENGTH = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * large element bit length
     */
    private static final int LARGE_ELEMENT_BIT_LENGTH = 160000;
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
        Alpr21SingleIndexPirConfig mulpirConfig = new Alpr21SingleIndexPirConfig.Builder().build();
        // Mul PIR (1-dimension)
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.MUL_PIR.name() + " (1-dimension)",
            mulpirConfig,
            new Alpr21SingleIndexPirParams(
                4096,
                20,
                1
            )
        });
        // Mul PIR (2-dimension)
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.MUL_PIR.name() + " (2-dimension)",
            mulpirConfig,
            new Alpr21SingleIndexPirParams(
                8192,
                20,
                2
            )
        });
        // Mul PIR (3-dimension)
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.MUL_PIR.name() + " (3-dimension)",
            mulpirConfig,
            new Alpr21SingleIndexPirParams(
                8192,
                20,
                3
            )
        });
        return configurations;
    }

    /**
     * Mul PIR config
     */
    private final Alpr21SingleIndexPirConfig indexPirConfig;
    /**
     * Mul PIR params
     */
    private final Alpr21SingleIndexPirParams indexPirParams;

    public MulPirTest(String name, Alpr21SingleIndexPirConfig indexPirConfig, Alpr21SingleIndexPirParams indexPirParams) {
        super(name);
        this.indexPirConfig = indexPirConfig;
        this.indexPirParams = indexPirParams;
    }

    @Test
    public void testMulPir() {
        testMulPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, false);
    }

    @Test
    public void testParallelMulPir() {
        testMulPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testLargeElementMulPir() {
        testMulPir(indexPirConfig, indexPirParams, LARGE_ELEMENT_BIT_LENGTH, true);
    }

    @Test
    public void testSmallElementMulPir() {
        testMulPir(indexPirConfig, indexPirParams, SMALL_ELEMENT_BIT_LENGTH, true);
    }

    public void testMulPir(Alpr21SingleIndexPirConfig config, Alpr21SingleIndexPirParams indexPirParams,
                           int elementBitLength, boolean parallel) {
        int retrievalSingleIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementBitLength);
        Alpr21SingleIndexPirServer server = new Alpr21SingleIndexPirServer(firstRpc, secondRpc.ownParty(), config);
        Alpr21SingleIndexPirClient client = new Alpr21SingleIndexPirClient(secondRpc, firstRpc.ownParty(), config);
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

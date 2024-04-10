package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.fastpir.Ayaa21SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir.Alpr21SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.onionpir.Mcr21SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.sealpir.Acls18SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.Hhcm23SimpleSingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.vectorizedpir.Mr23SingleIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.single.xpir.Mbfk16SingleIndexPirConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * index PIR test.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
@RunWith(Parameterized.class)
public class IndexPirTest extends AbstractTwoPartyMemoryRpcPto {
    /**
     * default element bit length
     */
    private static final int DEFAULT_ELEMENT_BIT_LENGTH = Double.SIZE;
    /**
     * server element size
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 14;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // XPIR
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.XPIR.name(),
            new Mbfk16SingleIndexPirConfig.Builder().build()
        });
        // SEAL PIR
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.SEAL_PIR.name(),
            new Acls18SingleIndexPirConfig.Builder().build()
        });
        // OnionPIR
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.ONION_PIR.name(),
            new Mcr21SingleIndexPirConfig.Builder().build()
        });
        // FastPIR
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.FAST_PIR.name(),
            new Ayaa21SingleIndexPirConfig.Builder().build()
        });
        // Vectorized PIR
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.VECTORIZED_PIR.name(),
            new Mr23SingleIndexPirConfig.Builder().build()
        });
        // MulPIR
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.MUL_PIR.name(),
            new Alpr21SingleIndexPirConfig.Builder().build()
        });
        // Simple PIR
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.SIMPLE_PIR.name(),
            new Hhcm23SimpleSingleIndexPirConfig.Builder().build()
        });
        // Double PIR
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.DOUBLE_PIR.name(),
            new Hhcm23DoubleSingleIndexPirConfig.Builder().build()
        });
        // constant weight PIR
        configurations.add(new Object[]{
            SingleIndexPirFactory.SingleIndexPirType.CONSTANT_WEIGHT_PIR.name(),
            new Mk22SingleIndexPirConfig.Builder().build()
        });
        return configurations;
    }

    /**
     * index PIR config
     */
    private final SingleIndexPirConfig indexPirConfig;

    public IndexPirTest(String name, SingleIndexPirConfig indexPirConfig) {
        super(name);
        this.indexPirConfig = indexPirConfig;
    }

    @Test
    public void testParallelIndexPir() {
        testIndexPir(indexPirConfig, DEFAULT_ELEMENT_BIT_LENGTH, SERVER_ELEMENT_SIZE, true);
    }

    @Test
    public void testIndexPir() {
        testIndexPir(indexPirConfig, DEFAULT_ELEMENT_BIT_LENGTH, SERVER_ELEMENT_SIZE, false);
    }

    public void testIndexPir(SingleIndexPirConfig config, int elementBitLength, int serverElementSize, boolean parallel) {
        int retrievalIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementBitLength);
        SingleIndexPirServer server = SingleIndexPirFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        SingleIndexPirClient client = SingleIndexPirFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        IndexPirServerThread serverThread = new IndexPirServerThread(server, database);
        IndexPirClientThread clientThread = new IndexPirClientThread(
            client, retrievalIndex, serverElementSize, elementBitLength
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
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
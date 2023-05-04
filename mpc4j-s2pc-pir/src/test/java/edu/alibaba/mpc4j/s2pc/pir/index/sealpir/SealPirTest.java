package edu.alibaba.mpc4j.s2pc.pir.index.sealpir;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.IndexPirFactory;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * SEAL PIR test.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
@RunWith(Parameterized.class)
public class SealPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SealPirTest.class);
    /**
     * default element byte length
     */
    private static final int DEFAULT_ELEMENT_BYTE_LENGTH = 64;
    /**
     * large element byte length
     */
    private static final int LARGE_ELEMENT_BYTE_LENGTH = 30000;
    /**
     * small element byte length
     */
    private static final int SMALL_ELEMENT_BYTE_LENGTH = 8;
    /**
     * database size
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        Acls18IndexPirConfig sealpirConfig = new Acls18IndexPirConfig();
        // SEAL PIR (1-dimension)
        configurations.add(new Object[]{
            IndexPirFactory.IndexPirType.SEAL_PIR.name() + " (1-dimension)",
            sealpirConfig,
            new Acls18IndexPirParams(
                4096,
                20,
                1
            )
        });
        // SEAL PIR (2-dimension)
        configurations.add(new Object[]{
            IndexPirFactory.IndexPirType.SEAL_PIR.name() + " (2-dimension)",
            sealpirConfig,
            new Acls18IndexPirParams(
                4096,
                20,
                2
            )
        });
        return configurations;
    }

    /**
     * server rpc
     */
    private final Rpc serverRpc;
    /**
     * client rpc
     */
    private final Rpc clientRpc;
    /**
     * SEAL PIR config
     */
    private final Acls18IndexPirConfig indexPirConfig;
    /**
     * SEAL PIR params
     */
    private final Acls18IndexPirParams indexPirParams;

    public SealPirTest(String name, Acls18IndexPirConfig indexPirConfig, Acls18IndexPirParams indexPirParams) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        serverRpc = rpcManager.getRpc(0);
        clientRpc = rpcManager.getRpc(1);
        this.indexPirConfig = indexPirConfig;
        this.indexPirParams = indexPirParams;
    }

    @Before
    public void connect() {
        serverRpc.connect();
        clientRpc.connect();
    }

    @After
    public void disconnect() {
        serverRpc.disconnect();
        clientRpc.disconnect();
    }

    @Test
    public void testSealPir() {
        testSealPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testParallelSealPir() {
        testSealPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, true);
    }

    @Test
    public void testLargeElementSealPir() {
        testSealPir(indexPirConfig, indexPirParams, LARGE_ELEMENT_BYTE_LENGTH, true);
    }

    @Test
    public void testSmallElementSealPir() {
        testSealPir(indexPirConfig, indexPirParams, SMALL_ELEMENT_BYTE_LENGTH, true);
    }

    public void testSealPir(Acls18IndexPirConfig config, Acls18IndexPirParams indexPirParams, int elementByteLength,
                            boolean parallel) {
        int retrievalIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementByteLength * Byte.SIZE);
        Acls18IndexPirServer server = new Acls18IndexPirServer(serverRpc, clientRpc.ownParty(), config);
        Acls18IndexPirClient client = new Acls18IndexPirClient(clientRpc, serverRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        SealPirServerThread serverThread = new SealPirServerThread(server, indexPirParams, database);
        SealPirClientThread clientThread = new SealPirClientThread(
            client, indexPirParams, retrievalIndex, SERVER_ELEMENT_SIZE, elementByteLength
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            LOGGER.info("Server: The Communication costs {}MB", serverRpc.getSendByteLength() * 1.0 / (1024 * 1024));
            serverRpc.reset();
            LOGGER.info("Client: The Communication costs {}MB", clientRpc.getSendByteLength() * 1.0 / (1024 * 1024));
            clientRpc.reset();
            LOGGER.info("Parameters: \n {}", indexPirParams.toString());
            // verify result
            ByteBuffer result = clientThread.getRetrievalResult();
            Assert.assertEquals(
                result, ByteBuffer.wrap(database.getBytesData(retrievalIndex))
            );
            LOGGER.info("Client: The Retrieval Result is Correct");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.destroy();
        client.destroy();
    }
}
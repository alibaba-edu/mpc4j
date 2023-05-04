package edu.alibaba.mpc4j.s2pc.pir.index.onionpir;

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
 * OnionPIR test.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
@RunWith(Parameterized.class)
public class OnionPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnionPirTest.class);
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
        Mcr21IndexPirConfig onionpirConfig = new Mcr21IndexPirConfig();
        // first dimension is 32
        configurations.add(new Object[]{
            IndexPirFactory.IndexPirType.ONION_PIR.name() + " (first dimension 32)",
            onionpirConfig,
            new Mcr21IndexPirParams(
                32
            )
        });
        // first dimension is 128
        configurations.add(new Object[]{
            IndexPirFactory.IndexPirType.ONION_PIR.name() + " (first dimension 128)",
            onionpirConfig,
            new Mcr21IndexPirParams(
                128
            )
        });
        // first dimension is 256
        configurations.add(new Object[]{
            IndexPirFactory.IndexPirType.ONION_PIR.name() + " (first dimension 256)",
            onionpirConfig,
            new Mcr21IndexPirParams(
                256
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
     * OnionPIR config
     */
    private final Mcr21IndexPirConfig indexPirConfig;
    /**
     * OnionPIR params
     */
    private final Mcr21IndexPirParams indexPirParams;

    public OnionPirTest(String name, Mcr21IndexPirConfig indexPirConfig, Mcr21IndexPirParams indexPirParams) {
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
    public void testOnionPir() {
        testOnionPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testParallelOnionPir() {
        testOnionPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, true);
    }

    @Test
    public void testLargeElementOnionPir() {
        testOnionPir(indexPirConfig, indexPirParams, LARGE_ELEMENT_BYTE_LENGTH, true);
    }

    @Test
    public void testSmallElementOnionPir() {
        testOnionPir(indexPirConfig, indexPirParams, SMALL_ELEMENT_BYTE_LENGTH, true);
    }

    public void testOnionPir(Mcr21IndexPirConfig config, Mcr21IndexPirParams indexPirParams, int elementByteLength,
                             boolean parallel) {
        int retrievalIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementByteLength * Byte.SIZE);
        Mcr21IndexPirServer server = new Mcr21IndexPirServer(serverRpc, clientRpc.ownParty(), config);
        Mcr21IndexPirClient client = new Mcr21IndexPirClient(clientRpc, serverRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        OnionPirServerThread serverThread = new OnionPirServerThread(server, indexPirParams, database);
        OnionPirClientThread clientThread = new OnionPirClientThread(
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
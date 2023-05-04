package edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir;

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
 * Vectorized PIR测试类。
 *
 * @author Liqiang Peng
 * @date 2023/3/24
 */
@RunWith(Parameterized.class)
public class VectorizedPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorizedPirTest.class);
    /**
     * default element byte length
     */
    private static final int DEFAULT_ELEMENT_BYTE_LENGTH = 16;
    /**
     * large element byte length
     */
    private static final int LARGE_ELEMENT_BYTE_LENGTH = 64;
    /**
     * small element byte length
     */
    private static final int SMALL_ELEMENT_BYTE_LENGTH = 2;
    /**
     * database size
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 12;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();
        Mr23IndexPirConfig pirConfig = new Mr23IndexPirConfig();
        configurations.add(new Object[]{
            IndexPirFactory.IndexPirType.FAST_PIR.name(),
            pirConfig,
            new Mr23IndexPirParams(
                8192,
                20,
                64,
                20
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
     * Vectorized PIR config
     */
    private final Mr23IndexPirConfig indexPirConfig;
    /**
     * Vectorized PIR params
     */
    private final Mr23IndexPirParams indexPirParams;

    public VectorizedPirTest(String name, Mr23IndexPirConfig indexPirConfig, Mr23IndexPirParams indexPirParams) {
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
    public void testVectorizedPir() {
        testVectorizedPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testParallelVectorizedPir() {
        testVectorizedPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, true);
    }

    @Test
    public void testLargeElementVectorizedPir() {
        testVectorizedPir(indexPirConfig, indexPirParams, LARGE_ELEMENT_BYTE_LENGTH, true);
    }

    @Test
    public void testSmallElementVectorizedPir() {
        testVectorizedPir(indexPirConfig, indexPirParams, SMALL_ELEMENT_BYTE_LENGTH, true);
    }

    public void testVectorizedPir(Mr23IndexPirConfig config, Mr23IndexPirParams indexPirParams, int elementByteLength,
                                  boolean parallel) {
        int retrievalIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementByteLength * Byte.SIZE);
        Mr23IndexPirServer server = new Mr23IndexPirServer(serverRpc, clientRpc.ownParty(), config);
        Mr23IndexPirClient client = new Mr23IndexPirClient(clientRpc, serverRpc.ownParty(), config);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        VectorizedPirServerThread serverThread = new VectorizedPirServerThread(server, indexPirParams, database);
        VectorizedPirClientThread clientThread = new VectorizedPirClientThread(
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
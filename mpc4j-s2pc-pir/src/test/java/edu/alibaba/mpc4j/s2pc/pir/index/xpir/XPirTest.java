package edu.alibaba.mpc4j.s2pc.pir.index.xpir;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.*;
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
 * XPIR test.
 *
 * @author Liqiang Peng
 * @date 2022/8/26
 */
@RunWith(Parameterized.class)
public class XPirTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(XPirTest.class);
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
        Mbfk16IndexPirConfig xpirConfig = new Mbfk16IndexPirConfig();
        // XPIR (1-dimension)
        configurations.add(new Object[]{
            IndexPirFactory.IndexPirType.XPIR.name() + " (1-dimension)",
            xpirConfig,
            new Mbfk16IndexPirParams(
                4096,
                20,
                1
            )
        });
        // XPIR (2-dimension)
        configurations.add(new Object[]{
            IndexPirFactory.IndexPirType.XPIR.name() + " (2-dimension)",
            xpirConfig,
            new Mbfk16IndexPirParams(
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
     * XPIR config
     */
    private final Mbfk16IndexPirConfig indexPirConfig;
    /**
     * XPIR params
     */
    private final Mbfk16IndexPirParams indexPirParams;

    public XPirTest(String name, Mbfk16IndexPirConfig indexPirConfig, Mbfk16IndexPirParams indexPirParams) {
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
    public void testXPir() {
        testXPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, false);
    }

    @Test
    public void testParallelXPir() {
        testXPir(indexPirConfig, indexPirParams, DEFAULT_ELEMENT_BYTE_LENGTH, true);
    }

    @Test
    public void testLargeElementXPir() {
        testXPir(indexPirConfig, indexPirParams, LARGE_ELEMENT_BYTE_LENGTH, true);
    }

    @Test
    public void testSmallElementXPir() {
        testXPir(indexPirConfig, indexPirParams, SMALL_ELEMENT_BYTE_LENGTH, true);
    }

    public void testXPir(Mbfk16IndexPirConfig config, Mbfk16IndexPirParams indexPirParams, int elementByteLength,
                         boolean parallel) {
        int retrievalIndex = PirUtils.generateRetrievalIndex(SERVER_ELEMENT_SIZE);
        NaiveDatabase database = PirUtils.generateDataBase(SERVER_ELEMENT_SIZE, elementByteLength * Byte.SIZE);
        Mbfk16IndexPirServer server = new Mbfk16IndexPirServer(serverRpc, clientRpc.ownParty(), config);
        Mbfk16IndexPirClient client = new Mbfk16IndexPirClient(clientRpc, serverRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        XPirServerThread serverThread = new XPirServerThread(server, indexPirParams, database);
        XPirClientThread clientThread = new XPirClientThread(
            client, indexPirParams, retrievalIndex, SERVER_ELEMENT_SIZE, elementByteLength
        );
        try {
            serverThread.start();
            clientThread.start();
            serverThread.join();
            clientThread.join();
            LOGGER.info("Server: The Communication costs {}MB", serverRpc.getSendByteLength() * 1.0 / (1 << 20));
            serverRpc.reset();
            LOGGER.info("Client: The Communication costs {}MB", clientRpc.getSendByteLength() * 1.0 / (1 << 20));
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
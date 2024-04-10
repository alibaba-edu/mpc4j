package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * unbalanced circuit PSI server thread.
 *
 * @author Liqiang Peng
 * @date 2023/4/18
 */
public class UcpsiServerThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(UcpsiServerThread.class);
    /**
     * the server
     */
    private final UcpsiServer<ByteBuffer> server;
    /**
     * the server element set
     */
    private final Set<ByteBuffer> serverElementSet;
    /**
     * client element size
     */
    private final int clientElementSize;
    /**
     * the server output
     */
    private SquareZ2Vector serverOutput;

    UcpsiServerThread(UcpsiServer<ByteBuffer> server, Set<ByteBuffer> serverElementSet, int clientElementSize) {
        this.server = server;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    SquareZ2Vector getServerOutput() {
        return serverOutput;
    }

    @Override
    public void run() {
        try {
            server.init(serverElementSet, clientElementSize);
            LOGGER.info(
                "Server: The Offline Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().reset();
            server.getRpc().synchronize();
            serverOutput = server.psi();
            LOGGER.info(
                "Server: The Online Communication costs {}MB", server.getRpc().getSendByteLength() * 1.0 / (1 << 20)
            );
            server.getRpc().reset();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

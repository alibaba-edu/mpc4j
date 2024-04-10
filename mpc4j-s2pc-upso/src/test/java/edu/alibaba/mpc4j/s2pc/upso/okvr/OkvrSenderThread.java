package edu.alibaba.mpc4j.s2pc.upso.okvr;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * OKVR sender thread.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
class OkvrSenderThread extends Thread {
    /**
     * sender
     */
    private final OkvrSender sender;
    /**
     * value bit length
     */
    private final int l;
    /**
     * key-value map
     */
    private final Map<ByteBuffer, byte[]> keyValueMap;
    /**
     * retrieval size
     */
    private final int retrievalSize;

    OkvrSenderThread(OkvrSender sender, Map<ByteBuffer, byte[]> keyValueMap, int l, int retrievalSize) {
        this.sender = sender;
        this.l = l;
        this.keyValueMap = keyValueMap;
        this.retrievalSize = retrievalSize;
    }

    @Override
    public void run() {
        try {
            sender.init(keyValueMap, l, retrievalSize);
            sender.getRpc().synchronize();
            sender.okvr();
            sender.getRpc().synchronize();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
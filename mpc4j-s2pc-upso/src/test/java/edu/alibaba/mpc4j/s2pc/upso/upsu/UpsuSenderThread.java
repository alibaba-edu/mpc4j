package edu.alibaba.mpc4j.s2pc.upso.upsu;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * UPSU sender thread.
 *
 * @author Liqiang Peng
 * @date 2024/3/12
 */
public class UpsuSenderThread extends Thread {
    /**
     * UPSU sender
     */
    private final UpsuSender sender;
    /**
     * sender element set
     */
    private final Set<ByteBuffer> senderElementSet;
    /**
     * receiver element size
     */
    private final int receiverElementSize;
    /**
     * element byte length
     */
    private final int elementByteLength;

    UpsuSenderThread(UpsuSender sender, int receiverElementSize, Set<ByteBuffer> senderElementSet,
                     int elementByteLength) {
        this.sender = sender;
        this.receiverElementSize = receiverElementSize;
        this.senderElementSet = senderElementSet;
        this.elementByteLength = elementByteLength;
    }

    @Override
    public void run() {
        try {
            sender.init(senderElementSet.size(), receiverElementSize);
            sender.psu(senderElementSet, elementByteLength);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

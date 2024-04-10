package edu.alibaba.mpc4j.s2pc.upso.upsu;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;


/**
 * UPSU receiver thread.
 *
 * @author Liqiang Peng
 * @date 2024/3/12
 */
public class UpsuReceiverThread extends Thread {
    /**
     * UPSU receiver
     */
    private final UpsuReceiver receiver;
    /**
     * receiver element set
     */
    private final Set<ByteBuffer> receiverElementSet;
    /**
     * UPSU receiver output
     */
    private UpsuReceiverOutput upsuReceiverOutput;
    /**
     * sender element size
     */
    private final int senderElementSize;
    /**
     * element byte length
     */
    private final int elementByteLength;

    UpsuReceiverThread(UpsuReceiver receiver, int senderElementSize, Set<ByteBuffer> receiverElementSet,
                       int elementByteLength) {
        this.receiver = receiver;
        this.senderElementSize = senderElementSize;
        this.receiverElementSet = receiverElementSet;
        this.elementByteLength = elementByteLength;
    }

    UpsuReceiverOutput getUpsuReceiverOutput() {
        return upsuReceiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(receiverElementSet, senderElementSize, elementByteLength);
            upsuReceiverOutput = receiver.psu(senderElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

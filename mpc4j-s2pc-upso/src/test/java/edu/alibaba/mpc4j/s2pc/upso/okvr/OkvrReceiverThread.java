package edu.alibaba.mpc4j.s2pc.upso.okvr;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

/**
 * OKVR receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/3/26
 */
class OkvrReceiverThread extends Thread {
    /**
     * receiver
     */
    private final OkvrReceiver receiver;
    /**
     * point num
     */
    private final int num;
    /**
     * value bit length
     */
    private final int l;
    /**
     * retrieval keys
     */
    private final Set<ByteBuffer> keys;
    /**
     * retrieval values
     */
    private Map<ByteBuffer, byte[]> keyValueMap;

    OkvrReceiverThread(OkvrReceiver receiver, int num, int l, Set<ByteBuffer> keys) {
        this.receiver = receiver;
        this.num = num;
        this.l = l;
        this.keys = keys;
    }

    Map<ByteBuffer, byte[]> getKeyValueMap() {
        return keyValueMap;
    }

    @Override
    public void run() {
        try {
            receiver.init(num, l, keys.size());
            receiver.getRpc().synchronize();
            keyValueMap = receiver.okvr(keys);
            receiver.getRpc().synchronize();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
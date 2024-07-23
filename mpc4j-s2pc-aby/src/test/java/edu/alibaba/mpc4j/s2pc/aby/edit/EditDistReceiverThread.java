package edu.alibaba.mpc4j.s2pc.aby.edit;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Edit distance receiver thread.
 *
 * @author Li Peng
 * @date 2024/4/12
 */
public class EditDistReceiverThread extends Thread {

    private final DistCmpReceiver receiver;
    private final String[] data;
    private final int maxLength;

    EditDistReceiverThread(DistCmpReceiver receiver, String[] data, int maxLength) {
        this.receiver = receiver;
        this.data = data;
        this.maxLength = maxLength;
    }

    @Override
    public void run() {
        try {
            receiver.init(maxLength);
            receiver.editDist(data);
        } catch (MpcAbortException e) {
            throw new RuntimeException(e);
        }
    }
}

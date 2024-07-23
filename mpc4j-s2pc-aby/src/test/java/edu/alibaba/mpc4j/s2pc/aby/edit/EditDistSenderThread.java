package edu.alibaba.mpc4j.s2pc.aby.edit;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Edit distance sender thread.
 *
 * @author Li Peng
 * @date 2024/4/12
 */
public class EditDistSenderThread extends Thread {

    private final DistCmpSender sender;
    private final String[] data;
    private final int maxNum;
    private int[] res;

    EditDistSenderThread(DistCmpSender sender, String[] data, int maxNum) {
        this.sender = sender;
        this.data = data;
        this.maxNum = maxNum;
    }

    public int[] getRes() {
        return res;
    }

    @Override
    public void run() {
        try {
            sender.init(maxNum);
            res = sender.editDist(data);
        } catch (MpcAbortException e) {
            throw new RuntimeException(e);
        }
    }
}

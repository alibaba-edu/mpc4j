package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * NC-COT协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2021/01/26
 */
class NcCotReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final NcCotReceiver receiver;
    /**
     * 数量
     */
    private final int num;
    /**
     * 输出
     */
    private final CotReceiverOutput[] receiverOutputs;

    NcCotReceiverThread(NcCotReceiver receiver, int num, int round) {
        this.receiver = receiver;
        this.num = num;
        receiverOutputs = new CotReceiverOutput[round];
    }

    CotReceiverOutput[] getReceiverOutputs() {
        return receiverOutputs;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(num);
            for (int round = 0; round < receiverOutputs.length; round++) {
                receiverOutputs[round] = receiver.receive();
            }
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
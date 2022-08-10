package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * SSP-COT协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
class SspCotReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final SspCotReceiver receiver;
    /**
     * α
     */
    private final int alpha;
    /**
     * 数量
     */
    private final int num;
    /**
     * 预计算接收方输出
     */
    private final CotReceiverOutput preReceiverOutput;
    /**
     * 输出
     */
    private SspCotReceiverOutput receiverOutput;

    SspCotReceiverThread(SspCotReceiver receiver, int alpha, int num) {
        this(receiver, alpha, num, null);
    }

    SspCotReceiverThread(SspCotReceiver receiver, int alpha, int num, CotReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.alpha = alpha;
        this.num = num;
        this.preReceiverOutput = preReceiverOutput;
    }

    SspCotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(num);
            receiverOutput = preReceiverOutput == null ?
                receiver.receive(alpha, num) : receiver.receive(alpha, num, preReceiverOutput);
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

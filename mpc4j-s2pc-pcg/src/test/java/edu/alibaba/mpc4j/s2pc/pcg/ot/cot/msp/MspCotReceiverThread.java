package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotReceiverOutput;

/**
 * MSP-COT协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
class MspCotReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final MspCotReceiver receiver;
    /**
     * 多点索引值
     */
    private final int t;
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
    private MspCotReceiverOutput receiverOutput;

    MspCotReceiverThread(MspCotReceiver receiver, int t, int num) {
        this(receiver, t, num, null);
    }

    MspCotReceiverThread(MspCotReceiver receiver, int t, int num, CotReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.t = t;
        this.num = num;
        this.preReceiverOutput = preReceiverOutput;
    }

    MspCotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(t, num);
            receiverOutput = preReceiverOutput == null ?
                receiver.receive(t, num) : receiver.receive(t, num, preReceiverOutput);
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

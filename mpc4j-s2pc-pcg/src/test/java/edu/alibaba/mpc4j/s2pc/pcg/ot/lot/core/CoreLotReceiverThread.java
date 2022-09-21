package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.LotReceiverOutput;

/**
 * 核2^l选1-HOT协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/5/26
 */
class CoreLotReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final CoreLotReceiver receiver;
    /**
     * 输入比特长度
     */
    private final int inputBitLength;
    /**
     * 选择值数组
     */
    private final byte[][] choices;
    /**
     * 输出
     */
    private LotReceiverOutput receiverOutput;

    CoreLotReceiverThread(CoreLotReceiver receiver, int inputBitLength, byte[][] choices) {
        this.receiver = receiver;
        this.inputBitLength = inputBitLength;
        this.choices = choices;
    }

    LotReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().connect();
            receiver.init(inputBitLength, choices.length);
            receiverOutput = receiver.receive(choices);
            receiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

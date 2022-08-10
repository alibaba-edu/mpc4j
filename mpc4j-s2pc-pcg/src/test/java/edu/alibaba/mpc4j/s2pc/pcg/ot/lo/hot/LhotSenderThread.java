package edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * 2^l选1-HOT协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/5/26
 */
class LhotSenderThread extends Thread {
    /**
     * 发送方
     */
    private final LhotSender sender;
    /**
     * 输入比特长度
     */
    private final int inputBitLength;
    /**
     * 执行数量
     */
    private final int num;
    /**
     * 输出
     */
    private LhotSenderOutput senderOutput;

    LhotSenderThread(LhotSender sender, int inputBitLength, int num) {
        this.sender = sender;
        this.inputBitLength = inputBitLength;
        this.num = num;
    }

    LhotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(inputBitLength, num);
            senderOutput = sender.send(num);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

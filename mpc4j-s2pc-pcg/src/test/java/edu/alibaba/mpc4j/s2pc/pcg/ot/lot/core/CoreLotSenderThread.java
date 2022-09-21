package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * 核2^l选1-OT协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/5/26
 */
class CoreLotSenderThread extends Thread {
    /**
     * 发送方
     */
    private final CoreLotSender sender;
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
    private CoreLotSenderOutput senderOutput;

    CoreLotSenderThread(CoreLotSender sender, int inputBitLength, int num) {
        this.sender = sender;
        this.inputBitLength = inputBitLength;
        this.num = num;
    }

    CoreLotSenderOutput getSenderOutput() {
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

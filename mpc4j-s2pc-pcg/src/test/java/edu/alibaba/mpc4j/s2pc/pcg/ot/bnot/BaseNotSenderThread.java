package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * 基础n选1-OT协议发送方线程。
 *
 * @author Hanwen Feng
 * @date 2022/07/22
 */
class BaseNotSenderThread extends Thread{
    /**
     * 发送方
     */
    private final BaseNotSender sender;
    /**
     * 密钥数量
     */
    private final int num;
    /**
     * 最大选择数量
     */
    private final int maxChoice;
    /**
     * 输出
     */
    private BaseNotSenderOutput senderOutput;

    BaseNotSenderThread(BaseNotSender sender, int num, int maxChoice) {
        this.sender = sender;
        this.num = num;
        this.maxChoice = maxChoice;
    }

    BaseNotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(maxChoice);
            senderOutput = sender.send(num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

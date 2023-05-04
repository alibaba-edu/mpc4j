package edu.alibaba.mpc4j.s2pc.pcg.ot.base;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * 基础OT协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2019/07/12
 */
class BaseOtSenderThread extends Thread {
    /**
     * 发送方
     */
    private final BaseOtSender sender;
    /**
     * 密钥数量
     */
    private final int num;
    /**
     * 输出
     */
    private BaseOtSenderOutput senderOutput;

    BaseOtSenderThread(BaseOtSender sender, int num) {
        this.sender = sender;
        this.num = num;
    }

    BaseOtSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init();
            senderOutput = sender.send(num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
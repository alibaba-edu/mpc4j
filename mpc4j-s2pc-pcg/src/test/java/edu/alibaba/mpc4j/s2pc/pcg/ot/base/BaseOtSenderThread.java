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
    private final BaseOtSender baseOtSender;
    /**
     * 密钥数量
     */
    private final int num;
    /**
     * 输出
     */
    private BaseOtSenderOutput senderOutput;

    BaseOtSenderThread(BaseOtSender baseOtSender, int num) {
        this.baseOtSender = baseOtSender;
        this.num = num;
    }

    BaseOtSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            baseOtSender.getRpc().connect();
            baseOtSender.init();
            senderOutput = baseOtSender.send(num);
            baseOtSender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
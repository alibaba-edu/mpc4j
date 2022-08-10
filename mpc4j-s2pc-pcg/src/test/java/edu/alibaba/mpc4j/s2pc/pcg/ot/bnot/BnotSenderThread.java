package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * 基础N选1-OT协议发送方线程。
 *
 * @author Hanwen Feng
 * @date 2022/07/22
 */
class BnotSenderThread extends Thread{
    /**
     * 发送方
     */
    private final BnotSender sender;
    /**
     * 密钥数量
     */
    private final int num;
    /**
     * 选择数量
     */
    private final int n;
    /**
     * 输出
     */
    private BnotSenderOutput senderOutput;

    BnotSenderThread(BnotSender sender, int num, int n) {
        this.sender = sender;
        this.num = num;
        this.n = n;
    }

    BnotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(n);
            senderOutput = sender.send(num);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

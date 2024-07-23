package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * 2^l选1-COT协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/5/26
 */
class LcotSenderThread extends Thread {
    /**
     * 发送方
     */
    private final LcotSender sender;
    /**
     * 输入比特长度
     */
    private final int l;
    /**
     * 执行数量
     */
    private final int num;
    /**
     * 输出
     */
    private LcotSenderOutput senderOutput;

    LcotSenderThread(LcotSender sender, int l, int num) {
        this.sender = sender;
        this.l = l;
        this.num = num;
    }

    LcotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(l);
            senderOutput = sender.send(num);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

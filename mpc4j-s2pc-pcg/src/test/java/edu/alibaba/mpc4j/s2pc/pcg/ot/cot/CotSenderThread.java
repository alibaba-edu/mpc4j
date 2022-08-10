package edu.alibaba.mpc4j.s2pc.pcg.ot.cot;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * COT协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class CotSenderThread extends Thread {
    /**
     * 发送方
     */
    private final CotSender sender;
    /**
     * 关联值Δ
     */
    private final byte[] delta;
    /**
     * 数量
     */
    private final int num;
    /**
     * 输出
     */
    private CotSenderOutput senderOutput;

    CotSenderThread(CotSender sender, byte[] delta, int num) {
        this.sender = sender;
        this.delta = delta;
        this.num = num;
    }

    CotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(delta, num, num);
            senderOutput = sender.send(num);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

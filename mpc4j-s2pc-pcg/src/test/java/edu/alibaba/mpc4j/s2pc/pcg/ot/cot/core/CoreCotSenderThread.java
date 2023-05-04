package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * 核COT协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2021/01/26
 */
class CoreCotSenderThread extends Thread {
    /**
     * 发送方
     */
    private final CoreCotSender sender;
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

    CoreCotSenderThread(CoreCotSender sender, byte[] delta, int num) {
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
            sender.init(delta, num);
            senderOutput = sender.send(num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
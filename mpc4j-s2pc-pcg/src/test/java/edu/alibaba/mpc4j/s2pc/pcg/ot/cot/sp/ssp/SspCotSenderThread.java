package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * SSP-COT协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
class SspCotSenderThread extends Thread {
    /**
     * 发送方
     */
    private final SspCotSender sender;
    /**
     * 关联值Δ
     */
    private final byte[] delta;
    /**
     * 数量
     */
    private final int num;
    /**
     * 预计算发送方输出
     */
    private final CotSenderOutput preSenderOutput;
    /**
     * 输出
     */
    private SspCotSenderOutput senderOutput;

    SspCotSenderThread(SspCotSender sender, byte[] delta, int num) {
        this(sender, delta, num, null);
    }

    SspCotSenderThread(SspCotSender sender, byte[] delta, int num, CotSenderOutput preSenderOutput) {
        this.sender = sender;
        this.delta = delta;
        this.num = num;
        this.preSenderOutput = preSenderOutput;
    }

    SspCotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(delta, num);
            senderOutput = preSenderOutput == null ? sender.send(num) : sender.send(num, preSenderOutput);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

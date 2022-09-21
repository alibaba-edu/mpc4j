package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotSenderOutput;

/**
 * MSP-COT协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
class MspCotSenderThread extends Thread {
    /**
     * 发送方
     */
    private final MspCotSender sender;
    /**
     * 关联值Δ
     */
    private final byte[] delta;
    /**
     * 稀疏点数量
     */
    private final int t;
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
    private MspCotSenderOutput senderOutput;

    MspCotSenderThread(MspCotSender sender, byte[] delta, int t, int num) {
        this(sender, delta, t, num, null);
    }

    MspCotSenderThread(MspCotSender sender, byte[] delta, int t, int num, CotSenderOutput preSenderOutput) {
        this.sender = sender;
        this.delta = delta;
        this.t = t;
        this.num = num;
        this.preSenderOutput = preSenderOutput;
    }

    MspCotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(delta, t, num);
            senderOutput = preSenderOutput == null ? sender.send(t, num) : sender.send(t, num, preSenderOutput);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

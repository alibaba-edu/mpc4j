package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotSenderOutput;

/**
 * BSP-COT协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
class BspCotSenderThread extends Thread {
    /**
     * 发送方
     */
    private final BspCotSender sender;
    /**
     * 关联值Δ
     */
    private final byte[] delta;
    /**
     * 批处理数量
     */
    private final int batch;
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
    private BspCotSenderOutput senderOutput;

    BspCotSenderThread(BspCotSender sender, byte[] delta, int batch, int num) {
        this(sender, delta, batch, num, null);
    }

    BspCotSenderThread(BspCotSender sender, byte[] delta, int batch, int num, CotSenderOutput preSenderOutput) {
        this.sender = sender;
        this.delta = delta;
        this.batch = batch;
        this.num = num;
        this.preSenderOutput = preSenderOutput;
    }

    BspCotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(delta, batch, num);
            senderOutput = preSenderOutput == null ? sender.send(batch, num) : sender.send(batch, num, preSenderOutput);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

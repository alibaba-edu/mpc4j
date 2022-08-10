package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * NC-COT协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2021/01/26
 */
class NcCotSenderThread extends Thread {
    /**
     * 发送方
     */
    private final NcCotSender sender;
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
    private final CotSenderOutput[] senderOutputs;

    NcCotSenderThread(NcCotSender sender, byte[] delta, int num, int round) {
        this.sender = sender;
        this.delta = delta;
        this.num = num;
        senderOutputs = new CotSenderOutput[round];
    }

    CotSenderOutput[] getSenderOutputs() {
        return senderOutputs;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(delta, num);
            for (int round = 0; round < senderOutputs.length; round++) {
                senderOutputs[round] = sender.send();
            }
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

/**
 * DPPRF发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
class DpprfSenderThread extends Thread {
    /**
     * 发送方
     */
    private final DpprfSender sender;
    /**
     * 批处理数量
     */
    private final int batchNum;
    /**
     * α上界
     */
    private final int alphaBound;
    /**
     * 预计算发送方输出
     */
    private final CotSenderOutput preSenderOutput;
    /**
     * 输出
     */
    private DpprfSenderOutput senderOutput;

    DpprfSenderThread(DpprfSender sender, int batchNum, int alphaBound) {
        this(sender, batchNum, alphaBound, null);
    }

    DpprfSenderThread(DpprfSender sender, int batchNum, int alphaBound, CotSenderOutput preSenderOutput) {
        this.sender = sender;
        this.batchNum = batchNum;
        this.alphaBound = alphaBound;
        this.preSenderOutput = preSenderOutput;
    }

    DpprfSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(batchNum, alphaBound);
            senderOutput = preSenderOutput == null ? sender.puncture(batchNum, alphaBound)
                : sender.puncture(batchNum, alphaBound, preSenderOutput);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

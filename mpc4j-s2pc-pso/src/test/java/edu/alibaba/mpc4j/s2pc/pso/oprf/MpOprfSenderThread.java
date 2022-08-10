package edu.alibaba.mpc4j.s2pc.pso.oprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * MPOPRF发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/4/9
 */
class MpOprfSenderThread extends Thread {
    /**
     * 发送方
     */
    private final MpOprfSender mpOprfSender;
    /**
     * 批处理数量
     */
    private final int batchSize;
    /**
     * 输出
     */
    private MpOprfSenderOutput senderOutput;

    MpOprfSenderThread(MpOprfSender mpOprfSender, int batchSize) {
        this.mpOprfSender = mpOprfSender;
        this.batchSize = batchSize;
    }

    MpOprfSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            mpOprfSender.getRpc().connect();
            mpOprfSender.init(batchSize);
            senderOutput = mpOprfSender.oprf(batchSize);
            mpOprfSender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

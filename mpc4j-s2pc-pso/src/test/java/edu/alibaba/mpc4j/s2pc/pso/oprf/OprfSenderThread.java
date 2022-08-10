package edu.alibaba.mpc4j.s2pc.pso.oprf;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * OPRF协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2019/07/12
 */
class OprfSenderThread extends Thread {
    /**
     * 发送方
     */
    private final OprfSender oprfSender;
    /**
     * 批处理数量
     */
    private final int batchSize;
    /**
     * 输出
     */
    private OprfSenderOutput senderOutput;

    OprfSenderThread(OprfSender oprfSender, int batchSize) {
        this.oprfSender = oprfSender;
        this.batchSize = batchSize;
    }

    OprfSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            oprfSender.getRpc().connect();
            oprfSender.init(batchSize);
            senderOutput = oprfSender.oprf(batchSize);
            oprfSender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
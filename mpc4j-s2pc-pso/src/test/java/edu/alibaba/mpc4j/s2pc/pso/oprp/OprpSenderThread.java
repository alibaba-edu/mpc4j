package edu.alibaba.mpc4j.s2pc.pso.oprp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * OPRP协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
class OprpSenderThread extends Thread {
    /**
     * 发送方
     */
    private final OprpSender sender;
    /**
     * 发送方密钥
     */
    private final byte[] key;
    /**
     * 批处理数量
     */
    private final int batchSize;
    /**
     * 发送方输出
     */
    private OprpSenderOutput senderOutput;

    OprpSenderThread(OprpSender sender, byte[] key, int batchSize) {
        this.sender = sender;
        this.key = key;
        this.batchSize = batchSize;
    }

    OprpSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(batchSize);
            senderOutput = sender.oprp(key, batchSize);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

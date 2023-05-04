package edu.alibaba.mpc4j.s2pc.opf.oprp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpSender;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpSenderOutput;

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
            sender.init(batchSize);
            senderOutput = sender.oprp(key, batchSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

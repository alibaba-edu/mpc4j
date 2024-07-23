package edu.alibaba.mpc4j.s2pc.opf.oprp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;

/**
 * OPRP协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
class OprpSenderThread extends Thread {
    /**
     * circuit sender
     */
    private final Z2cParty z2cSender;
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

    OprpSenderThread(Z2cParty z2cSender, OprpSender sender, byte[] key, int batchSize) {
        this.z2cSender = z2cSender;
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
            z2cSender.init((int) Math.min(Integer.MAX_VALUE, OprpFactory.expectZ2TripleNum(sender.getType(), batchSize)));
            sender.init(batchSize);
            senderOutput = sender.oprp(key, batchSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

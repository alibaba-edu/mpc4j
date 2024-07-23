package edu.alibaba.mpc4j.s2pc.opf.oprp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;

/**
 * OPRP协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public class OprpReceiverThread extends Thread {
    /**
     * circuit receiver
     */
    private final Z2cParty z2cReceiver;
    /**
     * 接收方
     */
    private final OprpReceiver receiver;
    /**
     * 接收方消息
     */
    private final byte[][] messages;
    /**
     * 批处理数量
     */
    private final int batchSize;
    /**
     * 接收方输出
     */
    private OprpReceiverOutput receiverOutput;

    OprpReceiverThread(Z2cParty z2cReceiver, OprpReceiver receiver, byte[][] messages) {
        this.z2cReceiver = z2cReceiver;
        this.receiver = receiver;
        this.messages = messages;
        batchSize = messages.length;
    }

    OprpReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            z2cReceiver.init((int) Math.min(Integer.MAX_VALUE, OprpFactory.expectZ2TripleNum(receiver.getType(), batchSize)));
            receiver.init(batchSize);
            receiverOutput = receiver.oprp(messages);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

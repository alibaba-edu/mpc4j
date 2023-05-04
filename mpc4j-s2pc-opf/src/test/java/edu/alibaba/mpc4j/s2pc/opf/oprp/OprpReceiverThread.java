package edu.alibaba.mpc4j.s2pc.opf.oprp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpReceiverOutput;

/**
 * OPRP协议接收方线程。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public class OprpReceiverThread extends Thread {
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

    OprpReceiverThread(OprpReceiver receiver, byte[][] messages) {
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
            receiver.init(batchSize);
            receiverOutput = receiver.oprp(messages);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

package edu.alibaba.mpc4j.s2pc.opf.osn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;

/**
 * 不经意交换网络接收方线程。
 *
 * @author Weiran Liu
 * @date 2021/09/20
 */
public class OsnReceiverThread extends Thread {
    /**
     * 接收方
     */
    private final OsnReceiver receiver;
    /**
     * 输入/分享字节长度
     */
    private final int byteLength;
    /**
     * 交换方式
     */
    private final int[] permutationMap;
    /**
     * 输出
     */
    private OsnPartyOutput receiverOutput;

    OsnReceiverThread(OsnReceiver receiver, int[] permutationMap, int byteLength) {
        this.receiver = receiver;
        this.byteLength = byteLength;
        this.permutationMap = permutationMap;
    }

    OsnPartyOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(permutationMap.length);
            receiverOutput = receiver.osn(permutationMap, byteLength);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

package edu.alibaba.mpc4j.s2pc.opf.osn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;

import java.util.Vector;

/**
 * OSN协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/02/10
 */
public class OsnSenderThread extends Thread {
    /**
     * 发送方
     */
    private final OsnSender sender;
    /**
     * 输入/分享字节长度
     */
    private final int byteLength;
    /**
     * 发送方输入
     */
    private final Vector<byte[]> inputVector;
    /**
     * 输出
     */
    private OsnPartyOutput senderOutput;

    OsnSenderThread(OsnSender sender, Vector<byte[]> inputVector, int byteLength) {
        this.sender = sender;
        this.byteLength = byteLength;
        this.inputVector = inputVector;
    }

    OsnPartyOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.init(inputVector.size());
            senderOutput = sender.osn(inputVector, byteLength);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

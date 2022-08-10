package edu.alibaba.mpc4j.s2pc.pso.osn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

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
    private final OsnSender osnSender;
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

    OsnSenderThread(OsnSender osnSender, Vector<byte[]> inputVector, int byteLength) {
        this.osnSender = osnSender;
        this.byteLength = byteLength;
        this.inputVector = inputVector;
    }

    OsnPartyOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            osnSender.getRpc().connect();
            osnSender.init(inputVector.size());
            senderOutput = osnSender.osn(inputVector, byteLength);
            this.osnSender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

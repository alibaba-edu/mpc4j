package edu.alibaba.mpc4j.s2pc.pcg.ot.no;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * n选1-OT协议发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/5/26
 */
class NotSenderThread extends Thread {
    /**
     * 发送方
     */
    private final NotSender sender;
    /**
     * 最大选择值
     */
    private final int n;
    /**
     * 执行数量
     */
    private final int num;
    /**
     * 输出
     */
    private NotSenderOutput senderOutput;

    NotSenderThread(NotSender sender, int n, int num) {
        this.sender = sender;
        this.n = n;
        this.num = num;
    }

    NotSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(n, num);
            senderOutput = sender.send(num);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

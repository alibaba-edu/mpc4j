package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleSenderOutput;

/**
 * Z2-BSP-VOLE发送方线程。
 *
 * @author Weiran Liu
 * @date 2022/6/23
 */
class Z2BspVoleSenderThread extends Thread {
    /**
     * 发送方
     */
    private final Z2BspVoleSender sender;
    /**
     * α数组
     */
    private final int[] alphaArray;
    /**
     * 数量
     */
    private final int num;
    /**
     * 预计算发送方输出
     */
    private final Z2VoleSenderOutput preSenderOutput;
    /**
     * 输出
     */
    private Z2BspVoleSenderOutput senderOutput;

    Z2BspVoleSenderThread(Z2BspVoleSender sender, int[] alphaArray, int num) {
        this(sender, alphaArray, num, null);
    }

    Z2BspVoleSenderThread(Z2BspVoleSender sender, int[] alphaArray, int num, Z2VoleSenderOutput preSenderOutput) {
        this.sender = sender;
        this.alphaArray = alphaArray;
        this.num = num;
        this.preSenderOutput = preSenderOutput;
    }

    Z2BspVoleSenderOutput getSenderOutput() {
        return senderOutput;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().connect();
            sender.init(alphaArray.length, num);
            senderOutput = preSenderOutput == null ?
                sender.send(alphaArray, num) : sender.send(alphaArray, num, preSenderOutput);
            sender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

package edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Zl lookup table protocol receiver thread.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
public class ZlLutReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final ZlLutReceiver receiver;
    /**
     * xs
     */
    private final byte[][] inputs;
    /**
     * num
     */
    private final int num;
    /**
     * m
     */
    private final int m;
    /**
     * n
     */
    private final int n;
    /**
     * outputs
     */
    private byte[][] outputs;

    ZlLutReceiverThread(ZlLutReceiver receiver, byte[][] inputs, int m, int n) {
        this.receiver = receiver;
        this.inputs = inputs;
        num = inputs.length;
        this.m = m;
        this.n = n;
    }

    byte[][] getOutputs() {
        return outputs;
    }

    @Override
    public void run() {
        try {
            receiver.getRpc().synchronize();
            receiver.init(m, n, num);
            receiver.getRpc().reset();
            receiver.getRpc().synchronize();
            outputs = receiver.lookupTable(inputs, m, n);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
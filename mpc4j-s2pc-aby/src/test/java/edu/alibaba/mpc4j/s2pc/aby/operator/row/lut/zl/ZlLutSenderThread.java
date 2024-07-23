package edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Zl lookup table protocol sender thread.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
class ZlLutSenderThread extends Thread {
    /**
     * the sender
     */
    private final ZlLutSender sender;
    /**
     * table
     */
    private final byte[][][] table;
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

    ZlLutSenderThread(ZlLutSender sender, byte[][][] table, int m, int n) {
        this.sender = sender;
        this.table = table;
        num = table.length;
        this.m = m;
        this.n = n;
    }

    @Override
    public void run() {
        try {
            sender.getRpc().synchronize();
            sender.init(m, n, num);
            sender.getRpc().reset();
            sender.getRpc().synchronize();
            sender.lookupTable(table, m, n);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
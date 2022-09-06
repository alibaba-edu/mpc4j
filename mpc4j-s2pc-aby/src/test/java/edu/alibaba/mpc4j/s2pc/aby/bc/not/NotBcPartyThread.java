package edu.alibaba.mpc4j.s2pc.aby.bc.not;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcSquareVector;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcParty;

/**
 * NOT-BC协议参与方线程。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
class NotBcPartyThread extends Thread {
    /**
     * BC协议参与方
     */
    private final BcParty bcParty;
    /**
     * xi
     */
    private final BcSquareVector xi;
    /**
     * 运算数量
     */
    private final int num;
    /**
     * zi
     */
    private BcSquareVector zi;

    NotBcPartyThread(BcParty bcParty, BcSquareVector xi) {
        this.bcParty = bcParty;
        this.xi = xi;
        num = xi.bitLength();
    }

    BcSquareVector getPartyOutput() {
        return zi;
    }

    @Override
    public void run() {
        try {
            bcParty.getRpc().connect();
            bcParty.init(num, num);
            zi = bcParty.not(xi);
            bcParty.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

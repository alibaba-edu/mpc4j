package edu.alibaba.mpc4j.s2pc.aby.pcg;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * Trust Dealer thread.
 *
 * @author Weiran Liu
 * @date 2024/6/28
 */
public class TrustDealerThread extends Thread {
    /**
     * trust dealer
     */
    private final TrustDealer trustDealer;

    public TrustDealerThread(TrustDealer trustDealer) {
        this.trustDealer = trustDealer;
    }

    @Override
    public void run() {
        try {
            trustDealer.init();
            trustDealer.aid();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

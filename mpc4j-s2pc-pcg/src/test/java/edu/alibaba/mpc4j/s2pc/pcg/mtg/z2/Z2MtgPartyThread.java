package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

/**
 * 布尔三元组生成协议参与方线程。
 *
 * @author Weiran Liu
 * @date 2022/02/08
 */
class Z2MtgPartyThread extends Thread {
    /**
     * 参与方
     */
    private final Z2MtgParty party;
    /**
     * 布尔三元组数量
     */
    private final int num;
    /**
     * 输出
     */
    private Z2Triple output;

    Z2MtgPartyThread(Z2MtgParty party, int num) {
        this.party = party;
        this.num = num;
    }

    Z2Triple getOutput() {
        return output;
    }

    @Override
    public void run() {
        try {
            party.getRpc().connect();
            party.init(num, num);
            output = party.generate(num);
            party.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

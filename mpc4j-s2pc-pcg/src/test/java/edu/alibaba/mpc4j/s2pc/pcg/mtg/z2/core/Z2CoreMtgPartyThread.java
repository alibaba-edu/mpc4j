package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;

/**
 * 核布尔三元组生成协议参与方线程。
 *
 * @author Weiran Liu
 * @date 2022/7/13
 */
public class Z2CoreMtgPartyThread extends Thread {
    /**
     * 参与方
     */
    private final Z2CoreMtgParty party;
    /**
     * 布尔三元组数量
     */
    private final int num;
    /**
     * 输出
     */
    private Z2Triple output;

    Z2CoreMtgPartyThread(Z2CoreMtgParty party, int num) {
        this.party = party;
        this.num = num;
    }

    Z2Triple getOutput() {
        return output;
    }

    @Override
    public void run() {
        try {
            party.init(num);
            output = party.generate(num);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}

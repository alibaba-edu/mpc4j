package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.Zp64Triple;

/**
 * 核zp64三元组生成协议测试参与方线程。
 *
 * @author Liqiang Peng
 * @date 2022/9/7
 */
public class Zp64CoreMtgPartyThread extends Thread {
    /**
     * 参与方
     */
    private final Zp64CoreMtgParty party;
    /**
     * zp64三元组数量
     */
    private final int num;
    /**
     * 输出
     */
    private Zp64Triple output;

    Zp64CoreMtgPartyThread(Zp64CoreMtgParty party, int num) {
        this.party = party;
        this.num = num;
    }

    Zp64Triple getOutput() {
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

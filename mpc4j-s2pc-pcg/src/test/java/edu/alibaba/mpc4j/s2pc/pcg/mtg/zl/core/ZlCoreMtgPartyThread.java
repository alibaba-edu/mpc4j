package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlTriple;

/**
 * 核l比特三元组生成协议参与方线程。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public class ZlCoreMtgPartyThread extends Thread {
    /**
     * 参与方
     */
    private final ZlCoreMtgParty party;
    /**
     * 布尔三元组数量
     */
    private final int num;
    /**
     * 输出
     */
    private ZlTriple output;

    ZlCoreMtgPartyThread(ZlCoreMtgParty party, int num) {
        this.party = party;
        this.num = num;
    }

    ZlTriple getOutput() {
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

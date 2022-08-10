package edu.alibaba.mpc4j.s2pc.pcg.ot.no.lh2n;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * 2^l选1-HOT转换为n选1-OT协议信息。
 *
 * @author Weiran Liu
 * @date 2022/5/27
 */
public class Lh2nNotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)3789791143842404895L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "LH2N_NOT";

    /**
     * 单例模式
     */
    private static final Lh2nNotPtoDesc INSTANCE = new Lh2nNotPtoDesc();

    /**
     * 私有构造函数
     */
    private Lh2nNotPtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }
}

package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np99;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;


/**
 * NP99-基础n选1-OT协议信息。该论文提供使用2选1-OT构造n选1-OT的方案。论文来源：
 * <p>
 * Naor, Moni, and Benny Pinkas. Oblivious transfer and polynomial evaluation. STOC 1999.
 * </p>
 *
 * @author Hanwen Feng
 * @date 2022/07/19
 */
public class Np99BaseNotPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 4300807284283819586L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "NP99_BASE_NOT";
    /**
     * 单例模式
     */
    private static final Np99BaseNotPtoDesc INSTANCE = new Np99BaseNotPtoDesc();

    /**
     * 私有构造函数
     */
    private Np99BaseNotPtoDesc() {
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
